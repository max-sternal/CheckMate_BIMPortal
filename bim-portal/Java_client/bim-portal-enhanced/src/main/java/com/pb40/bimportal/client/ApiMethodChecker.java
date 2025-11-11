package com.pb40.bimportal.client;

import com.bimportal.client.api.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

/**
 * Utility to inspect generated API methods and capabilities This file should be placed in:
 * bim-portal-enhanced/src/main/java/com/pb40/bimportal/client/
 */
public class ApiMethodChecker {

  public static void main(String[] args) {
    System.out.println("======================================================================");
    System.out.println("BIM PORTAL API METHOD INSPECTOR");
    System.out.println("======================================================================");

    // List of API classes to inspect
    Class<?>[] apiClasses = {
      InfrastrukturApi.class,
      AiaProjekteApi.class,
      AiaLoinApi.class,
      AiaVorlagenApi.class,
      FachmodelleApi.class,
      KontextinformationenApi.class,
      MerkmaleApi.class,
      MerkmalsgruppenApi.class,
      AiaFilterApi.class
    };

    for (Class<?> apiClass : apiClasses) {
      inspectApiClass(apiClass);
    }

    System.out.println("======================================================================");
    System.out.println("INSPECTION COMPLETED");
    System.out.println("======================================================================");
  }

  private static void inspectApiClass(Class<?> apiClass) {
    System.out.println("\n=== " + apiClass.getSimpleName() + " ===");

    Method[] methods = apiClass.getDeclaredMethods();

    if (methods.length == 0) {
      System.out.println("  No methods found");
      return;
    }

    Arrays.sort(methods, (a, b) -> a.getName().compareTo(b.getName()));

    for (Method method : methods) {
      System.out.println("  " + formatMethod(method));
    }

    System.out.println("  Total methods: " + methods.length);
  }

  private static String formatMethod(Method method) {
    StringBuilder sb = new StringBuilder();

    // Return type
    String returnType = method.getReturnType().getSimpleName();
    sb.append(returnType).append(" ");

    // Method name
    sb.append(method.getName()).append("(");

    // Parameters
    Parameter[] params = method.getParameters();
    for (int i = 0; i < params.length; i++) {
      if (i > 0) sb.append(", ");
      sb.append(params[i].getType().getSimpleName());
      sb.append(" ").append(params[i].getName());
    }

    sb.append(")");

    return sb.toString();
  }
}
