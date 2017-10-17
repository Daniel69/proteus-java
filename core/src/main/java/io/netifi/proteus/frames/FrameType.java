package io.netifi.proteus.frames;

/**
 *
 */
public enum FrameType {
    UNDEFINED(0x00),
    DESTINATION_SETUP(0x01),
    ROUTER_SETUP(0x02),
    QUERY_SETUP(0x03),
    REQUEST_SHARED_SECRET(0x04),
    SHARED_SECRET(0x05),
    ROUTE(0x06),
    QUERY_DESTINATION_AVAIL(0x07),
    DESTINATION_AVAIL_RESULT(0x08),
    AUTH_REQUEST(0x09),
    AUTH_RESPONSE(0x0A),
    EXTENSION_FRAME(0x7F);

    private static FrameType[] typesById;
    
    private final int id;
    private final int flags;
    
    /** Index types by id for indexed lookup. */
    static {
        int max = 0;
        
        for (FrameType t : values()) {
            max = Math.max(t.id, max);
        }
        
        typesById = new FrameType[max + 1];
        
        for (FrameType t : values()) {
            typesById[t.id] = t;
        }
    }
    
    FrameType(final int id) {
        this(id, 0);
    }
    
    FrameType(int id, int flags) {
        this.id = id;
        this.flags = flags;
    }
    
    public int getEncodedType() {
        return id;
    }
    
    public static FrameType from(int id) {
        return typesById[id];
    }
}
