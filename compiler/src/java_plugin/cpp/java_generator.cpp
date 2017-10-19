#include "java_generator.h"

#include <algorithm>
#include <iostream>
#include <iterator>
#include <map>
#include <vector>
#include <google/protobuf/compiler/java/java_names.h>
#include <google/protobuf/descriptor.h>
#include <google/protobuf/io/printer.h>
#include <google/protobuf/io/zero_copy_stream.h>

// Stringify helpers used solely to cast PROTEUS_VERSION
#ifndef STR
#define STR(s) #s
#endif

#ifndef XSTR
#define XSTR(s) STR(s)
#endif

#ifndef FALLTHROUGH_INTENDED
#define FALLTHROUGH_INTENDED
#endif

namespace java_proteus_generator {

using google::protobuf::FileDescriptor;
using google::protobuf::ServiceDescriptor;
using google::protobuf::MethodDescriptor;
using google::protobuf::Descriptor;
using google::protobuf::io::Printer;
using google::protobuf::SourceLocation;
using std::to_string;

const int32_t Prime = 0x01000193; //   16777619
const int32_t Seed  = 0x811C9DC5; // 2166136261

// FNV-1 hash
static inline int32_t Hash(const string& word, int32_t hash = Seed) {
  const unsigned char* ptr = reinterpret_cast<const unsigned char *>(word.c_str());
  while (*ptr)
    hash = (*ptr++ ^ hash) * Prime;
  return hash;
}

// Adjust a method name prefix identifier to follow the JavaBean spec:
//   - decapitalize the first letter
//   - remove embedded underscores & capitalize the following letter
static string MixedLower(const string& word) {
  string w;
  w += tolower(word[0]);
  bool after_underscore = false;
  for (size_t i = 1; i < word.length(); ++i) {
    if (word[i] == '_') {
      after_underscore = true;
    } else {
      w += after_underscore ? toupper(word[i]) : word[i];
      after_underscore = false;
    }
  }
  return w;
}

// Converts to the identifier to the ALL_UPPER_CASE format.
//   - An underscore is inserted where a lower case letter is followed by an
//     upper case letter.
//   - All letters are converted to upper case
static string ToAllUpperCase(const string& word) {
  string w;
  for (size_t i = 0; i < word.length(); ++i) {
    w += toupper(word[i]);
    if ((i < word.length() - 1) && islower(word[i]) && isupper(word[i + 1])) {
      w += '_';
    }
  }
  return w;
}

static inline string LowerMethodName(const MethodDescriptor* method) {
  return MixedLower(method->name());
}

static inline string MethodIdFieldName(const MethodDescriptor* method) {
  return "METHOD_" + ToAllUpperCase(method->name());
}

static inline string MethodId(const MethodDescriptor* method) {
  int32_t hash = Hash(method->full_name());
  return to_string(hash);
}

static inline string MessageFullJavaName(const Descriptor* desc) {
  return google::protobuf::compiler::java::ClassName(desc);
}

static inline string ServiceIdFieldName(const ServiceDescriptor* service) { return "SERVICE_ID"; }

static inline string ServiceId(const ServiceDescriptor* service) {
  int32_t hash = Hash(service->name());
  return to_string(hash);
}

static inline string NamespaceIdFieldName(const ServiceDescriptor* service) { return "NAMESPACE_ID"; }

static inline string NamespaceId(const ServiceDescriptor* service) {
  int32_t hash = Hash(service->file()->package());
  return to_string(hash);
}

template <typename ITR>
static void SplitStringToIteratorUsing(const string& full,
                                       const char* delim,
                                       ITR& result) {
  // Optimize the common case where delim is a single character.
  if (delim[0] != '\0' && delim[1] == '\0') {
    char c = delim[0];
    const char* p = full.data();
    const char* end = p + full.size();
    while (p != end) {
      if (*p == c) {
        ++p;
      } else {
        const char* start = p;
        while (++p != end && *p != c);
        *result++ = string(start, p - start);
      }
    }
    return;
  }

  string::size_type begin_index, end_index;
  begin_index = full.find_first_not_of(delim);
  while (begin_index != string::npos) {
    end_index = full.find_first_of(delim, begin_index);
    if (end_index == string::npos) {
      *result++ = full.substr(begin_index);
      return;
    }
    *result++ = full.substr(begin_index, (end_index - begin_index));
    begin_index = full.find_first_not_of(delim, end_index);
  }
}

static void SplitStringUsing(const string& full,
                             const char* delim,
                             std::vector<string>* result) {
  std::back_insert_iterator< std::vector<string> > it(*result);
  SplitStringToIteratorUsing(full, delim, it);
}

static std::vector<string> Split(const string& full, const char* delim) {
  std::vector<string> result;
  SplitStringUsing(full, delim, &result);
  return result;
}

static string EscapeJavadoc(const string& input) {
  string result;
  result.reserve(input.size() * 2);

  char prev = '*';

  for (string::size_type i = 0; i < input.size(); i++) {
    char c = input[i];
    switch (c) {
      case '*':
        // Avoid "/*".
        if (prev == '/') {
          result.append("&#42;");
        } else {
          result.push_back(c);
        }
        break;
      case '/':
        // Avoid "*/".
        if (prev == '*') {
          result.append("&#47;");
        } else {
          result.push_back(c);
        }
        break;
      case '@':
        // '@' starts javadoc tags including the @deprecated tag, which will
        // cause a compile-time error if inserted before a declaration that
        // does not have a corresponding @Deprecated annotation.
        result.append("&#64;");
        break;
      case '<':
        // Avoid interpretation as HTML.
        result.append("&lt;");
        break;
      case '>':
        // Avoid interpretation as HTML.
        result.append("&gt;");
        break;
      case '&':
        // Avoid interpretation as HTML.
        result.append("&amp;");
        break;
      case '\\':
        // Java interprets Unicode escape sequences anywhere!
        result.append("&#92;");
        break;
      default:
        result.push_back(c);
        break;
    }

    prev = c;
  }

  return result;
}

template <typename DescriptorType>
static string GetCommentsForDescriptor(const DescriptorType* descriptor) {
  SourceLocation location;
  if (descriptor->GetSourceLocation(&location)) {
    return location.leading_comments.empty() ?
      location.trailing_comments : location.leading_comments;
  }
  return string();
}

static std::vector<string> GetDocLines(const string& comments) {
  if (!comments.empty()) {
    // Ideally we should parse the comment text as Markdown and
    // write it back as HTML, but this requires a Markdown parser.  For now
    // we just use <pre> to get fixed-width text formatting.

    // If the comment itself contains block comment start or end markers,
    // HTML-escape them so that they don't accidentally close the doc comment.
    string escapedComments = EscapeJavadoc(comments);

    std::vector<string> lines = Split(escapedComments, "\n");
    while (!lines.empty() && lines.back().empty()) {
      lines.pop_back();
    }
    return lines;
  }
  return std::vector<string>();
}

template <typename DescriptorType>
static std::vector<string> GetDocLinesForDescriptor(const DescriptorType* descriptor) {
  return GetDocLines(GetCommentsForDescriptor(descriptor));
}

static void WriteDocCommentBody(Printer* printer,
                                    const std::vector<string>& lines,
                                    bool surroundWithPreTag) {
  if (!lines.empty()) {
    if (surroundWithPreTag) {
      printer->Print(" * <pre>\n");
    }

    for (size_t i = 0; i < lines.size(); i++) {
      // Most lines should start with a space.  Watch out for lines that start
      // with a /, since putting that right after the leading asterisk will
      // close the comment.
      if (!lines[i].empty() && lines[i][0] == '/') {
        printer->Print(" * $line$\n", "line", lines[i]);
      } else {
        printer->Print(" *$line$\n", "line", lines[i]);
      }
    }

    if (surroundWithPreTag) {
      printer->Print(" * </pre>\n");
    }
  }
}

static void WriteDocComment(Printer* printer, const string& comments) {
  printer->Print("/**\n");
  std::vector<string> lines = GetDocLines(comments);
  WriteDocCommentBody(printer, lines, false);
  printer->Print(" */\n");
}

static void WriteServiceDocComment(Printer* printer,
                                       const ServiceDescriptor* service) {
  // Deviating from protobuf to avoid extraneous docs
  // (see https://github.com/google/protobuf/issues/1406);
  printer->Print("/**\n");
  std::vector<string> lines = GetDocLinesForDescriptor(service);
  WriteDocCommentBody(printer, lines, true);
  printer->Print(" */\n");
}

void WriteMethodDocComment(Printer* printer,
                           const MethodDescriptor* method) {
  // Deviating from protobuf to avoid extraneous docs
  // (see https://github.com/google/protobuf/issues/1406);
  printer->Print("/**\n");
  std::vector<string> lines = GetDocLinesForDescriptor(method);
  WriteDocCommentBody(printer, lines, true);
  printer->Print(" */\n");
}

enum CallType {
  ASYNC_CALL = 0,
  BLOCKING_CALL = 1,
  FUTURE_CALL = 2
};

static void PrintInterface(const ServiceDescriptor* service,
                           std::map<string, string>* vars,
                           Printer* p,
                           ProtoFlavor flavor,
                           bool disable_version) {
  (*vars)["service_name"] = service->name();
  (*vars)["namespace_id_name"] = NamespaceIdFieldName(service);
  (*vars)["namespace_id"] = NamespaceId(service);
  (*vars)["service_id_name"] = ServiceIdFieldName(service);
  (*vars)["service_id"] = ServiceId(service);
  (*vars)["file_name"] = service->file()->name();
  (*vars)["proteus_version"] = "";
  #ifdef PROTEUS_VERSION
  if (!disable_version) {
    (*vars)["proteus_version"] = " (version " XSTR(PROTEUS_VERSION) ")";
  }
  #endif
  WriteServiceDocComment(p, service);
  p->Print(
      *vars,
      "@$Generated$(\n"
      "    value = \"by Proteus proto compiler$proteus_version$\",\n"
      "    comments = \"Source: $file_name$\")\n"
      "public interface $service_name$ extends $Availability$, $Closeable$ {\n");
  p->Indent();

  // Service IDs
  p->Print(*vars, "int $namespace_id_name$ = $namespace_id$;\n");
  p->Print(*vars, "int $service_id_name$ = $service_id$;\n");

  for (int i = 0; i < service->method_count(); ++i) {
    const MethodDescriptor* method = service->method(i);
    (*vars)["method_id_name"] = MethodIdFieldName(method);
    (*vars)["method_id"] = MethodId(method);

    p->Print(*vars, "int $method_id_name$ = $method_id$;\n");
  }

  // RPC methods
  for (int i = 0; i < service->method_count(); ++i) {
    const MethodDescriptor* method = service->method(i);
    (*vars)["input_type"] = MessageFullJavaName(method->input_type());
    (*vars)["output_type"] = MessageFullJavaName(method->output_type());
    (*vars)["lower_method_name"] = LowerMethodName(method);
    bool client_streaming = method->client_streaming();
    bool server_streaming = method->server_streaming();

    // Method signature
    p->Print("\n");
    WriteMethodDocComment(p, method);

    if (server_streaming) {
      p->Print(*vars, "$Flux$<$output_type$> $lower_method_name$");
    } else if (client_streaming) {
      p->Print(*vars, "$Mono$<$output_type$> $lower_method_name$");
    } else {
      const Descriptor* output_type = method->output_type();
      if (output_type->field_count() > 0) {
        p->Print(*vars, "$Mono$<$output_type$> $lower_method_name$");
      } else {
        p->Print(*vars, "$Mono$<Void> $lower_method_name$");
      }
    }
    if (client_streaming) {
      // Bidirectional streaming or client streaming
      p->Print(*vars, "($Publisher$<$input_type$> messages);\n");
    } else {
      // Server streaming or simple RPC
      p->Print(*vars, "($input_type$ message);\n");
    }
  }

  p->Outdent();
  p->Print("}\n");
}

static void PrintClient(const ServiceDescriptor* service,
                        std::map<string, string>* vars,
                        Printer* p,
                        ProtoFlavor flavor,
                        bool disable_version) {
  (*vars)["service_name"] = service->name();
  (*vars)["namespace_id_name"] = NamespaceIdFieldName(service);
  (*vars)["service_id_name"] = ServiceIdFieldName(service);
  (*vars)["file_name"] = service->file()->name();
  (*vars)["client_class_name"] = ClientClassName(service);
  (*vars)["proteus_version"] = "";
  #ifdef PROTEUS_VERSION
  if (!disable_version) {
    (*vars)["proteus_version"] = " (version " XSTR(PROTEUS_VERSION) ")";
  }
  #endif
  p->Print(
      *vars,
      "@$Generated$(\n"
      "    value = \"by Proteus proto compiler$proteus_version$\",\n"
      "    comments = \"Source: $file_name$\")\n"
      "public final class $client_class_name$ implements $service_name$ {\n");
  p->Indent();

  p->Print(
      *vars,
      "private final $RSocket$ rSocket;\n\n"
      "public $client_class_name$($RSocket$ rSocket) {\n");
  p->Indent();
  p->Print(
      *vars,
      "this.rSocket = rSocket;\n");
  p->Outdent();
  p->Print("}\n\n");

  // RPC methods
  for (int i = 0; i < service->method_count(); ++i) {
    const MethodDescriptor* method = service->method(i);
    (*vars)["input_type"] = MessageFullJavaName(method->input_type());
    (*vars)["output_type"] = MessageFullJavaName(method->output_type());
    (*vars)["lower_method_name"] = LowerMethodName(method);
    (*vars)["method_id_name"] = MethodIdFieldName(method);
    bool client_streaming = method->client_streaming();
    bool server_streaming = method->server_streaming();

    // Method signature
    if (server_streaming) {
      p->Print(
          *vars,
          "@$Override$\n"
          "public $Flux$<$output_type$> $lower_method_name$");
    } else if (client_streaming) {
      p->Print(
          *vars,
          "@$Override$\n"
          "public $Mono$<$output_type$> $lower_method_name$");
    } else {
      const Descriptor* output_type = method->output_type();
      if (output_type->field_count() > 0) {
        p->Print(
            *vars,
            "@$Override$\n"
            "public $Mono$<$output_type$> $lower_method_name$");
      } else {
        p->Print(
            *vars,
            "@$Override$\n"
            "public $Mono$<Void> $lower_method_name$");
      }
    }
    if (client_streaming) {
      // Bidirectional streaming or client streaming
      p->Print(
          *vars,
          "($Publisher$<$input_type$> messages) {\n");
      p->Indent();
      p->Print(
          *vars,
          "int length = $ProteusMetadata$.computeLength();\n"
          "final $ByteBuf$ metadata = $ByteBufAllocator$.DEFAULT.directBuffer(length);\n"
          "$ProteusMetadata$.encode(metadata, $service_name$.$namespace_id_name$, $service_name$.$service_id_name$, $service_name$.$method_id_name$);\n\n"
          "$Flux$<$input_type$> publisher = $Flux$.$from$(messages);\n"
          "return rSocket.requestChannel(publisher.map(new $Function$<$MessageLite$, $Payload$>() {\n");
      p->Indent();
      p->Print(
          *vars,
          "@$Override$\n"
          "public $Payload$ apply($MessageLite$ message) {\n");
      p->Indent();
      p->Print(
          *vars,
          "$ByteBuffer$ data = message.toByteString().asReadOnlyByteBuffer();\n"
          "return new $PayloadImpl$(data, metadata.nioBuffer());\n");
      p->Outdent();
      p->Print("}\n");
      p->Outdent();
      if (server_streaming) {
        p->Print(
            *vars,
            "})).map(deserializer($output_type$.parser()));\n");
      } else {
        p->Print(
            *vars,
            "})).map(deserializer($output_type$.parser())).single();\n");
      }
      p->Outdent();
      p->Print("}\n\n");
    } else {
      // Server streaming or simple RPC
      p->Print(
          *vars,
          "($input_type$ message) {\n");
      p->Indent();
      p->Print(
          *vars,
          "int length = $ProteusMetadata$.computeLength();\n"
          "$ByteBuf$ metadata = $ByteBufAllocator$.DEFAULT.directBuffer(length);\n"
          "$ProteusMetadata$.encode(metadata, $service_name$.$namespace_id_name$, $service_name$.$service_id_name$, $service_name$.$method_id_name$);\n"
          "$ByteBuffer$ data = message.toByteString().asReadOnlyByteBuffer();\n\n");

      if (server_streaming) {
        p->Print(
            *vars,
            "return rSocket.requestStream(new $PayloadImpl$(data, metadata.nioBuffer()))\n");
        p->Indent();
        p->Print(
            *vars,
            ".map(deserializer($output_type$.parser()));\n");
        p->Outdent();
      } else {
        const Descriptor* output_type = method->output_type();
        if (output_type->field_count() > 0) {
          p->Print(
              *vars,
              "return rSocket.requestResponse(new $PayloadImpl$(data, metadata.nioBuffer()))\n");
          p->Indent();
          p->Print(
              *vars,
              ".map(deserializer($output_type$.parser()));\n");
          p->Outdent();
        } else {
          p->Print(
              *vars,
              "return rSocket.fireAndForget(new $PayloadImpl$(data, metadata.nioBuffer()));\n");
        }
      }

      p->Outdent();
      p->Print("}\n\n");
    }
  }

  // Availability
  p->Print(
      *vars,
      "@$Override$\n"
      "public double availability() {\n");
  p->Indent();
  p->Print(
      *vars,
      "return rSocket.availability();\n");
  p->Outdent();
  p->Print("}\n\n");

  // Closeable
  p->Print(
      *vars,
      "@$Override$\n"
      "public $Mono$<Void> close() {\n");
  p->Indent();
  p->Print(
      *vars,
      "return rSocket.close();\n");
  p->Outdent();
  p->Print("}\n\n");
  p->Print(
      *vars,
      "@$Override$\n"
      "public $Mono$<Void> onClose() {\n");
  p->Indent();
  p->Print(
      *vars,
      "return rSocket.onClose();\n");
  p->Outdent();
  p->Print("}\n\n");

  // Deserializer
  p->Print(
      *vars,
      "private static <T> $Function$<$Payload$, T> deserializer(final $Parser$<T> parser) {\n");
  p->Indent();
  p->Print(
      *vars,
      "return new $Function$<$Payload$, T>() {\n");
  p->Indent();
  p->Print(
      *vars,
      "@$Override$\n"
      "public T apply($Payload$ payload) {\n");
  p->Indent();
  p->Print(
      *vars,
      "try {\n");
  p->Indent();
  p->Print(
      *vars,
      "$ByteString$ data = $UnsafeByteOperations$.unsafeWrap(payload.getData());\n"
      "return parser.parseFrom(data);\n");
  p->Outdent();
  p->Print("} catch (Throwable t) {\n");
  p->Indent();
  p->Print(
      *vars,
      "throw new RuntimeException(t);\n");
  p->Outdent();
  p->Print("}\n");
  p->Outdent();
  p->Print("}\n");
  p->Outdent();
  p->Print("};\n");
  p->Outdent();
  p->Print("}\n");

  p->Outdent();
  p->Print("}\n");
}

static void PrintServer(const ServiceDescriptor* service,
                        std::map<string, string>* vars,
                        Printer* p,
                        ProtoFlavor flavor,
                        bool disable_version) {
  (*vars)["service_name"] = service->name();
  (*vars)["namespace_id_name"] = NamespaceIdFieldName(service);
  (*vars)["service_id_name"] = ServiceIdFieldName(service);
  (*vars)["file_name"] = service->file()->name();
  (*vars)["server_class_name"] = ServerClassName(service);
  (*vars)["proteus_version"] = "";
  #ifdef PROTEUS_VERSION
  if (!disable_version) {
    (*vars)["proteus_version"] = " (version " XSTR(PROTEUS_VERSION) ")";
  }
  #endif
  p->Print(
      *vars,
      "@$Generated$(\n"
      "    value = \"by Proteus proto compiler$proteus_version$\",\n"
      "    comments = \"Source: $file_name$\")\n"
      "public final class $server_class_name$ implements $ProteusService$ {\n");
  p->Indent();

  p->Print(
      *vars,
      "private final $service_name$ service;\n\n"
      "public $server_class_name$($service_name$ service) {\n");
  p->Indent();
  p->Print(
      *vars,
      "this.service = service;\n");
  p->Outdent();
  p->Print("}\n\n");

  p->Print(
      *vars,
      "@$Override$\n"
      "public int getNamespaceId() {\n");
  p->Indent();
  p->Print(
      *vars,
      "return $service_name$.$namespace_id_name$;\n");
  p->Outdent();
  p->Print("}\n\n");

  p->Print(
      *vars,
      "@$Override$\n"
      "public int getServiceId() {\n");
  p->Indent();
  p->Print(
      *vars,
      "return $service_name$.$service_id_name$;\n");
  p->Outdent();
  p->Print("}\n\n");

  std::vector<const MethodDescriptor*> fire_and_forget;
  std::vector<const MethodDescriptor*> request_response;
  std::vector<const MethodDescriptor*> request_stream;
  std::vector<const MethodDescriptor*> request_channel;

  for (int i = 0; i < service->method_count(); ++i) {
    const MethodDescriptor* method = service->method(i);
    bool client_streaming = method->client_streaming();
    bool server_streaming = method->server_streaming();

    if (client_streaming) {
      request_channel.push_back(method);
    } else if (server_streaming) {
      request_stream.push_back(method);
    } else {
      const Descriptor* output_type = method->output_type();
      if (output_type->field_count() > 0) {
        request_response.push_back(method);
      } else {
        fire_and_forget.push_back(method);
      }
    }
  }

  // Fire and forget
  p->Print(
      *vars,
      "@$Override$\n"
      "public $Mono$<Void> fireAndForget($Payload$ payload) {\n");
  p->Indent();
  if (fire_and_forget.empty()) {
    p->Print(
        *vars,
        "return $Mono$.error(new UnsupportedOperationException(\"Fire and forget not implemented.\"));\n");
  } else {
    p->Print(
        *vars,
        "try {\n");
    p->Indent();
    p->Print(
        *vars,
        "$ByteBuf$ metadata = $Unpooled$.wrappedBuffer(payload.getMetadata());\n"
        "switch($ProteusMetadata$.methodId(metadata)) {\n");
    p->Indent();
    for (vector<const MethodDescriptor*>::iterator it = fire_and_forget.begin(); it != fire_and_forget.end(); ++it) {
      const MethodDescriptor* method = *it;
      (*vars)["input_type"] = MessageFullJavaName(method->input_type());
      (*vars)["lower_method_name"] = LowerMethodName(method);
      (*vars)["method_id_name"] = MethodIdFieldName(method);
      p->Print(
          *vars,
          "case $service_name$.$method_id_name$: {\n");
      p->Indent();
      p->Print(
          *vars,
          "$ByteString$ data = $UnsafeByteOperations$.unsafeWrap(payload.getData());\n"
          "return service.$lower_method_name$($input_type$.parseFrom(data));\n");
      p->Outdent();
      p->Print("}\n");
    }
    p->Print(
        *vars,
        "default: {\n");
    p->Indent();
    p->Print(
        *vars,
        "return $Mono$.error(new UnsupportedOperationException());\n");
    p->Outdent();
    p->Print("}\n");
    p->Outdent();
    p->Print("}\n");
    p->Outdent();
    p->Print("} catch (Throwable t) {\n");
    p->Indent();
    p->Print(
        *vars,
        "return $Mono$.error(t);\n");
    p->Outdent();
    p->Print("}\n");
  }
  p->Outdent();
  p->Print("}\n\n");

  // Request-Response
  p->Print(
      *vars,
      "@$Override$\n"
      "public $Mono$<$Payload$> requestResponse($Payload$ payload) {\n");
  p->Indent();
  if (request_response.empty()) {
    p->Print(
        *vars,
        "return $Mono$.error(new UnsupportedOperationException(\"Request-Response not implemented.\"));\n");
  } else {
    p->Print(
        *vars,
        "try {\n");
    p->Indent();
    p->Print(
        *vars,
        "$ByteBuf$ metadata = $Unpooled$.wrappedBuffer(payload.getMetadata());\n"
        "switch($ProteusMetadata$.methodId(metadata)) {\n");
    p->Indent();
    for (vector<const MethodDescriptor*>::iterator it = request_response.begin(); it != request_response.end(); ++it) {
      const MethodDescriptor* method = *it;
      (*vars)["input_type"] = MessageFullJavaName(method->input_type());
      (*vars)["output_type"] = MessageFullJavaName(method->output_type());
      (*vars)["lower_method_name"] = LowerMethodName(method);
      (*vars)["method_id_name"] = MethodIdFieldName(method);
      p->Print(
          *vars,
          "case $service_name$.$method_id_name$: {\n");
      p->Indent();
      p->Print(
          *vars,
          "$ByteString$ data = $UnsafeByteOperations$.unsafeWrap(payload.getData());\n"
          "return service.$lower_method_name$($input_type$.parseFrom(data)).map(serializer);\n");
      p->Outdent();
      p->Print("}\n");
    }
    p->Print(
        *vars,
        "default: {\n");
    p->Indent();
    p->Print(
        *vars,
        "return $Mono$.error(new UnsupportedOperationException());\n");
    p->Outdent();
    p->Print("}\n");
    p->Outdent();
    p->Print("}\n");
    p->Outdent();
    p->Print("} catch (Throwable t) {\n");
    p->Indent();
    p->Print(
        *vars,
        "return $Mono$.error(t);\n");
    p->Outdent();
    p->Print("}\n");
  }
  p->Outdent();
  p->Print("}\n\n");

  // Request-Stream
  p->Print(
      *vars,
      "@$Override$\n"
      "public $Flux$<$Payload$> requestStream($Payload$ payload) {\n");
  p->Indent();
  if (request_stream.empty()) {
    p->Print(
        *vars,
        "return $Flux$.error(new UnsupportedOperationException(\"Request-Stream not implemented.\"));\n");
  } else {
    p->Print(
        *vars,
        "try {\n");
    p->Indent();
    p->Print(
        *vars,
        "$ByteBuf$ metadata = $Unpooled$.wrappedBuffer(payload.getMetadata());\n"
        "switch($ProteusMetadata$.methodId(metadata)) {\n");
    p->Indent();
    for (vector<const MethodDescriptor*>::iterator it = request_stream.begin(); it != request_stream.end(); ++it) {
      const MethodDescriptor* method = *it;
      (*vars)["input_type"] = MessageFullJavaName(method->input_type());
      (*vars)["output_type"] = MessageFullJavaName(method->output_type());
      (*vars)["lower_method_name"] = LowerMethodName(method);
      (*vars)["method_id_name"] = MethodIdFieldName(method);
      p->Print(
          *vars,
          "case $service_name$.$method_id_name$: {\n");
      p->Indent();
      p->Print(
          *vars,
          "$ByteString$ data = $UnsafeByteOperations$.unsafeWrap(payload.getData());\n"
          "return service.$lower_method_name$($input_type$.parseFrom(data)).map(serializer);\n");
      p->Outdent();
      p->Print("}\n");
    }
    p->Print(
        *vars,
        "default: {\n");
    p->Indent();
    p->Print(
        *vars,
        "return $Flux$.error(new UnsupportedOperationException());\n");
    p->Outdent();
    p->Print("}\n");
    p->Outdent();
    p->Print("}\n");
    p->Outdent();
    p->Print("} catch (Throwable t) {\n");
    p->Indent();
    p->Print(
        *vars,
        "return $Flux$.error(t);\n");
    p->Outdent();
    p->Print("}\n");
  }
  p->Outdent();
  p->Print("}\n\n");

  // Request-Channel
  p->Print(
      *vars,
      "@$Override$\n"
      "public $Flux$<$Payload$> requestChannel($Publisher$<$Payload$> payloads) {\n");
  p->Indent();
  if (request_channel.empty()) {
    p->Print(
        *vars,
        "return $Flux$.error(new UnsupportedOperationException(\"Request-Channel not implemented.\"));\n");
  } else {
    p->Print(
        *vars,
        "final $Flux$<$Payload$> publisher = $Flux$.$from$(payloads);\n"
        "return publisher.$next$().$flatMap$(new $Function$<$Payload$, $Publisher$<? extends $MessageLite$>>() {\n");
    p->Indent();
    p->Print(
        *vars,
        "@$Override$\n"
        "public $Publisher$<? extends $MessageLite$> apply($Payload$ payload) {\n");
    p->Indent();
    p->Print(
        *vars,
        "try {\n");
    p->Indent();
    p->Print(
        *vars,
        "$ByteBuf$ metadata = $Unpooled$.wrappedBuffer(payload.getMetadata());\n"
        "switch($ProteusMetadata$.methodId(metadata)) {\n");
    p->Indent();
    for (vector<const MethodDescriptor*>::iterator it = request_channel.begin(); it != request_channel.end(); ++it) {
      const MethodDescriptor* method = *it;
      (*vars)["input_type"] = MessageFullJavaName(method->input_type());
      (*vars)["output_type"] = MessageFullJavaName(method->output_type());
      (*vars)["lower_method_name"] = LowerMethodName(method);
      (*vars)["method_id_name"] = MethodIdFieldName(method);
      p->Print(
          *vars,
          "case $service_name$.$method_id_name$: {\n");
      p->Indent();
      p->Print(
          *vars,
          "$Flux$<$input_type$> messages =\n");
      p->Indent();
      p->Print(
          *vars,
          "publisher.map(deserializer($input_type$.parser()));\n");
      p->Outdent();
      p->Print(
          *vars,
          "return service.$lower_method_name$(messages);\n");
      p->Outdent();
      p->Print("}\n");
    }
    p->Print(
        *vars,
        "default: {\n");
    p->Indent();
    p->Print(
        *vars,
        "return $Flux$.error(new UnsupportedOperationException());\n");
    p->Outdent();
    p->Print("}\n");
    p->Outdent();
    p->Print("}\n");
    p->Outdent();
    p->Print("} catch (Throwable t) {\n");
    p->Indent();
    p->Print(
        *vars,
        "return $Flux$.error(t);\n");
    p->Outdent();
    p->Print("}\n");
    p->Outdent();
    p->Print("}\n");
    p->Outdent();
    p->Print(
        *vars,
        "}).map(serializer);\n");
  }
  p->Outdent();
  p->Print("}\n\n");

  // Metadata-Push
  p->Print(
      *vars,
      "@$Override$\n"
      "public $Mono$<Void> metadataPush($Payload$ payload) {\n");
  p->Indent();
  p->Print(
      *vars,
      "return $Mono$.error(new UnsupportedOperationException(\"Metadata-Push not implemented.\"));\n");
  p->Outdent();
  p->Print("}\n\n");

  // Availability
  p->Print(
      *vars,
      "@$Override$\n"
      "public double availability() {\n");
  p->Indent();
  p->Print(
      *vars,
      "return service.availability();\n");
  p->Outdent();
  p->Print("}\n\n");

  // Closeable
  p->Print(
      *vars,
      "@$Override$\n"
      "public $Mono$<Void> close() {\n");
  p->Indent();
  p->Print(
      *vars,
      "return service.close();\n");
  p->Outdent();
  p->Print("}\n\n");
  p->Print(
      *vars,
      "@$Override$\n"
      "public $Mono$<Void> onClose() {\n");
  p->Indent();
  p->Print(
      *vars,
      "return service.onClose();\n");
  p->Outdent();
  p->Print("}\n\n");

  // Serializer
  p->Print(
      *vars,
      "private static final $Function$<$MessageLite$, $Payload$> serializer =\n");
  p->Indent();
  p->Print(
      *vars,
      "new $Function$<$MessageLite$, $Payload$>() {\n");
  p->Indent();
  p->Print(
      *vars,
      "@$Override$\n"
      "public $Payload$ apply($MessageLite$ message) {\n");
  p->Indent();
  p->Print(
      *vars,
      "return new $PayloadImpl$(message.toByteString().asReadOnlyByteBuffer());\n");
  p->Outdent();
  p->Print("}\n");
  p->Outdent();
  p->Print("};\n\n");
  p->Outdent();

  // Deserializer
  p->Print(
      *vars,
      "private static <T> $Function$<$Payload$, T> deserializer(final $Parser$<T> parser) {\n");
  p->Indent();
  p->Print(
      *vars,
      "return new $Function$<$Payload$, T>() {\n");
  p->Indent();
  p->Print(
      *vars,
      "@$Override$\n"
      "public T apply($Payload$ payload) {\n");
  p->Indent();
  p->Print(
      *vars,
      "try {\n");
  p->Indent();
  p->Print(
      *vars,
      "$ByteString$ data = $UnsafeByteOperations$.unsafeWrap(payload.getData());\n"
      "return parser.parseFrom(data);\n");
  p->Outdent();
  p->Print("} catch (Throwable t) {\n");
  p->Indent();
  p->Print(
      *vars,
      "throw new RuntimeException(t);\n");
  p->Outdent();
  p->Print("}\n");
  p->Outdent();
  p->Print("}\n");
  p->Outdent();
  p->Print("};\n");
  p->Outdent();
  p->Print("}\n");

  p->Outdent();
  p->Print("}\n");
}

void GenerateInterface(const ServiceDescriptor* service,
                       google::protobuf::io::ZeroCopyOutputStream* out,
                       ProtoFlavor flavor,
                       bool disable_version) {
  // All non-generated classes must be referred by fully qualified names to
  // avoid collision with generated classes.
  std::map<string, string> vars;
  vars["Flux"] = "reactor.core.publisher.Flux";
  vars["Mono"] = "reactor.core.publisher.Mono";
  vars["Publisher"] = "org.reactivestreams.Publisher";
  vars["Generated"] = "javax.annotation.Generated";
  vars["Availability"] = "io.rsocket.Availability";
  vars["Closeable"] = "io.rsocket.Closeable";

  Printer printer(out, '$');
  string package_name = ServiceJavaPackage(service->file());
  if (!package_name.empty()) {
    printer.Print(
        "package $package_name$;\n\n",
        "package_name", package_name);
  }

  // Package string is used to fully qualify method names.
  vars["Package"] = service->file()->package();
  if (!vars["Package"].empty()) {
    vars["Package"].append(".");
  }
  PrintInterface(service, &vars, &printer, flavor, disable_version);
}

void GenerateClient(const ServiceDescriptor* service,
                    google::protobuf::io::ZeroCopyOutputStream* out,
                    ProtoFlavor flavor,
                    bool disable_version) {
  // All non-generated classes must be referred by fully qualified names to
  // avoid collision with generated classes.
  std::map<string, string> vars;
  vars["Flux"] = "reactor.core.publisher.Flux";
  vars["Mono"] = "reactor.core.publisher.Mono";
  vars["from"] = "from";
  vars["Function"] = "java.util.function.Function";
  vars["Override"] = "java.lang.Override";
  vars["Publisher"] = "org.reactivestreams.Publisher";
  vars["Generated"] = "javax.annotation.Generated";
  vars["RSocket"] = "io.rsocket.RSocket";
  vars["Payload"] = "io.rsocket.Payload";
  vars["PayloadImpl"] = "io.rsocket.util.PayloadImpl";
  vars["ByteBuf"] = "io.netty.buffer.ByteBuf";
  vars["ByteBufAllocator"] = "io.netty.buffer.ByteBufAllocator";
  vars["ByteBuffer"] = "java.nio.ByteBuffer";
  vars["ProteusMetadata"] = "io.netifi.proteus.frames.ProteusMetadata";
  vars["ByteString"] = "com.google.protobuf.ByteString";
  vars["UnsafeByteOperations"] = "com.google.protobuf.UnsafeByteOperations";
  vars["MessageLite"] = "com.google.protobuf.MessageLite";
  vars["Parser"] = "com.google.protobuf.Parser";

  Printer printer(out, '$');
  string package_name = ServiceJavaPackage(service->file());
  if (!package_name.empty()) {
    printer.Print(
        "package $package_name$;\n\n",
        "package_name", package_name);
  }

  // Package string is used to fully qualify method names.
  vars["Package"] = service->file()->package();
  if (!vars["Package"].empty()) {
    vars["Package"].append(".");
  }
  PrintClient(service, &vars, &printer, flavor, disable_version);
}

void GenerateServer(const ServiceDescriptor* service,
                    google::protobuf::io::ZeroCopyOutputStream* out,
                    ProtoFlavor flavor,
                    bool disable_version) {
  // All non-generated classes must be referred by fully qualified names to
  // avoid collision with generated classes.
  std::map<string, string> vars;
  vars["Flux"] = "reactor.core.publisher.Flux";
  vars["Mono"] = "reactor.core.publisher.Mono";
  vars["from"] = "from";
  vars["next"] = "next";
  vars["flatMap"] = "flatMapMany";
  vars["Function"] = "java.util.function.Function";
  vars["Override"] = "java.lang.Override";
  vars["Publisher"] = "org.reactivestreams.Publisher";
  vars["Generated"] = "javax.annotation.Generated";
  vars["RSocket"] = "io.rsocket.RSocket";
  vars["Payload"] = "io.rsocket.Payload";
  vars["PayloadImpl"] = "io.rsocket.util.PayloadImpl";
  vars["ProteusService"] = "io.netifi.proteus.ProteusService";
  vars["ProteusMetadata"] = "io.netifi.proteus.frames.ProteusMetadata";
  vars["ByteBuf"] = "io.netty.buffer.ByteBuf";
  vars["Unpooled"] = "io.netty.buffer.Unpooled";
  vars["ByteString"] = "com.google.protobuf.ByteString";
  vars["UnsafeByteOperations"] = "com.google.protobuf.UnsafeByteOperations";
  vars["MessageLite"] = "com.google.protobuf.MessageLite";
  vars["Parser"] = "com.google.protobuf.Parser";

  Printer printer(out, '$');
  string package_name = ServiceJavaPackage(service->file());
  if (!package_name.empty()) {
    printer.Print(
        "package $package_name$;\n\n",
        "package_name", package_name);
  }

  // Package string is used to fully qualify method names.
  vars["Package"] = service->file()->package();
  if (!vars["Package"].empty()) {
    vars["Package"].append(".");
  }
  PrintServer(service, &vars, &printer, flavor, disable_version);
}

string ServiceJavaPackage(const FileDescriptor* file) {
  string result = google::protobuf::compiler::java::ClassName(file);
  size_t last_dot_pos = result.find_last_of('.');
  if (last_dot_pos != string::npos) {
    result.resize(last_dot_pos);
  } else {
    result = "";
  }
  return result;
}

string ClientClassName(const google::protobuf::ServiceDescriptor* service) {
  return service->name() + "Client";
}

string ServerClassName(const google::protobuf::ServiceDescriptor* service) {
  return service->name() + "Server";
}

}  // namespace java_proteus_generator
