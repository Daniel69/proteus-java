package io.netifi.proteus.exception;

public class ServiceNotFound extends RuntimeException {
  public ServiceNotFound(int packageId, int serviceId) {
    super("can not find service for package id " + packageId + " and service id " + serviceId);
  }
}
