/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.uima.internal.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.apache.uima.UIMARuntimeException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.function.Runnable_withException;

public class Misc {

  public static final String blanks = "                                                     ";
  public static final String dots = "...";
  
  private static final Pattern whitespace = Pattern.compile("\\s");

  public static String replaceWhiteSpace(String s, String replacement) {
    return whitespace.matcher(s).replaceAll(replacement);
  }
  
  
  /**
   * @param s starting frames above invoker
   * @param n max number of callers to return
   * @return  x called by: y ...
   */
  public static StringBuilder getCallers(final int s, final int n) {
    StackTraceElement[] e = Thread.currentThread().getStackTrace();
    StringBuilder sb = new StringBuilder();
    for (int i = s + 2; i < s + n + 3; i++) {
      if (i >= e.length) break;
      if (i != s + 2) {
        sb.append("  called_by: ");
      }
      sb.append(formatcaller(e[i]));      
    }
    return sb;
  }
  
  /**
   * @return the name of the caller in the stack
   */
  public static String getCaller() {
    StackTraceElement[] e = Thread.currentThread().getStackTrace();
    return formatcaller(e[4]) + "called by: " + formatcaller(e[5]);
  }
  
  private static String formatcaller(StackTraceElement e) {
    String n = e.getClassName();
    return n.substring(1 + n.lastIndexOf('.')) + "." + e.getMethodName() + "[" + e.getLineNumber() + "]";
  }

  public static String elide(String s, int n) {
    return elide(s, n, true);
  }
  
  public static String elide(String s, int n, boolean pad) {
    if (s == null) {
      s = "null";
    }
    int sl = s.length();
    if (sl <= n) {
      return s + (pad ? blanks.substring(0, n - sl) : "");
    }
    int dl = 1; //(n < 11) ? 1 : (n < 29) ? 2 : 3;  // number of dots
    int ss = (n - dl) / 2;
    int ss2 = ss + (( ss * 2 == (n - dl)) ? 0 : 1);
    return s.substring(0, ss) + dots.substring(0, dl) + s.substring(sl - ss2);
  }
  
  public final static MethodHandles.Lookup UIMAlookup = MethodHandles.lookup();
  
  /**
   * 
   * @param name of property
   * @return true if property is defined, or is defined and set to anything 
   * except "false"; false if property is not defined, or is defined and set to
   * "false".
   */
  public static boolean getNoValueSystemProperty(String name) {
    return !System.getProperty(name, "false").equals("false");
  }
  /**
   * For standardized prettyprinting, to string.
   * Adds a collection of things (toString) separated by , and surrounded by [  ], to a StringBuilder
   * @param sb where the formatted collection results are appended to 
   * @param c the collection
   * @return the StringBuilder for chaining
   */
  public static <T> StringBuilder addElementsToStringBuilder(StringBuilder sb, Collection<T> c){
    sb.append('[');
    String[] prefix = new String[] {""};
    c.stream().forEachOrdered(item -> {
       sb.append(prefix[0]);
       sb.append(item.toString());
       prefix[0] = ", ";
    });
    sb.append(']');
    return sb;
  }
  
  /**
   * For standardized prettyprinting, to string
   * Adds a collection of things (running an appender to append the result to the same sb) separated by , and surrounded by [  ], to a StringBuilder
   * @param sb where the formatted collection results are appended to 
   * @param c the collection
   * @return the StringBuilder for chaining
   */
  public static<T> StringBuilder addElementsToStringBuilder(StringBuilder sb, Collection<T> c, Consumer<T> appender){
    sb.append('[');
    String[] prefix = new String[] {""};
    c.stream().forEachOrdered(item -> {
       sb.append(prefix[0]);
       appender.accept(item);
       prefix[0] = ", ";
    });
    sb.append(']');
    return sb;
  }  
  
  /**
   * Writes a byte array output stream to a file
   * @param baos the array to write
   * @param name the name of the file
   */
  public static void toFile(ByteArrayOutputStream baos, String name) {
    try (FileOutputStream fos = new FileOutputStream(name)) {
      baos.writeTo(fos);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Writes a byte array output stream to a file
   * @param baos the array to write
   * @param name the file
   */
  public static void toFile(ByteArrayOutputStream baos, File file) {
    try (FileOutputStream fos = new FileOutputStream(file)) {
      baos.writeTo(fos);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * For multi-core machine "tuning" - the number of cores
   */
  public static final int numberOfCores = Runtime.getRuntime().availableProcessors();
  
  /**
   * Convert an int argument to the next higher power of 2 if not already a power of 2
   * @param i the value to convert 
   * @return the next higher power of 2, or i if it is already a power of 2
   */
  static public int nextHigherPowerOf2(int i) {
    return (i < 1) ? 1 : Integer.highestOneBit(i) << ( (Integer.bitCount(i) == 1 ? 0 : 1));
  }
  
  static public int nextHigherPowerOfX(int i, int x) {
    int shft = 31 - Integer.numberOfLeadingZeros(x);  // x == 8, shft = 3
    return (i < 1) ? x : ((i+(x - 1)) >>> shft) << shft;
  }
  
  /**
   * Given a class, a lookup context, and a protected method and its arg classes,
   * return the method handle for that method.
   * 
   * Using that method handle is slow, but converting it to a lambda makes for
   * JIT-able fast access.
   * 
   * @param clazz
   * @param methodHandleAccessContext
   * @param protectedMethod
   * @param args
   * @return
   */
  static public MethodHandle getProtectedMethodHandle(Class<?> clazz, Lookup methodHandleAccessContext, String protectedMethod, Class<?> ... args) {
    try {
      Method m = clazz.getDeclaredMethod(protectedMethod, args);
      m.setAccessible(true);    
      return methodHandleAccessContext.unreflect(m);
    } catch (NoSuchMethodException | SecurityException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }   
  }
  
  /**
   * Given a class, and a protected method and its arg classes,
   * return the method handle for that method.
   * 
   * Note: uses the UIMA context as the lookup context
   * 
   * Using that method handle is slow, but converting it to a lambda makes for
   * JIT-able fast access.
   * @param clazz
   * @param protectedMethod
   * @param args
   * @return
   */
  static public MethodHandle getProtectedMethodHandle(Class<?> clazz, String protectedMethod, Class<?> ... args) {
    return getProtectedMethodHandle(clazz, UIMAlookup, protectedMethod, args);
  }
  
  static public MethodHandle getProtectedFieldGetter(Class<?> clazz, String protectedField) {
    try {
      Field f = clazz.getDeclaredField(protectedField);
      f.setAccessible(true);
      return UIMAlookup.unreflectGetter(f);
    } catch (NoSuchFieldException | SecurityException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }  
  }
  
  /**
   * Gets an int from a named field.
   * If the field isn't present, returns Integer.MIN_VALUE;
   * @param clazz the class where the field is
   * @param fieldName the name of the field
   * @return the value or Integer.MIN_VALUE if not present
   */
  static public int getStaticIntField(Class<?> clazz, String fieldName) {
    try {
      Field f = clazz.getField(fieldName);
      return f.getInt(null);
    } catch (NoSuchFieldException e) {
      return Integer.MIN_VALUE;
    } catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
        throw new RuntimeException(e);
    } 
  }
  
  static public int getStaticIntFieldNoInherit(Class<?> clazz, String fieldName) {
    try {
      Field f = clazz.getDeclaredField(fieldName);
      return f.getInt(null);
    } catch (NoSuchFieldException e) {
      return Integer.MIN_VALUE;
    } catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
        throw new RuntimeException(e);
    } 
  }
  
  static public void addAll(Collection<String> c, String ... v) {
    for (String s : v) {
      c.add(s);
    }
  }
  
  public static void debug(Object o) {
    System.err.println("Debug: " + o);
  }
  
  /** 
   * Check and throw UIMA Internal Error if false
   * @param v if false, throws
   */
  public static void assertUie(boolean v) {
    if (!v) 
      throw new UIMARuntimeException(UIMARuntimeException.INTERNAL_ERROR);
  }
  
  public static void assertUie(boolean v, Throwable e) {
    if (!v) 
      throw new UIMARuntimeException(e, UIMARuntimeException.INTERNAL_ERROR);
  }
  
  public static void internalError() {
    assertUie(false);
  }
  
  public static void internalError(Throwable e) {
    assertUie(false, e);
  }
  
  // The hash function is derived from murmurhash3 32 bit, which
  // carries this statement:
  
  //  MurmurHash3 was written by Austin Appleby, and is placed in the public
  //  domain. The author hereby disclaims copyright to this source code.  
  
  // See also MurmurHash3 in wikipedia
  
  private static final int C1 = 0xcc9e2d51;
  private static final int C2 = 0x1b873593;
  private static final int seed = 0x39c2ab57;  // arbitrary bunch of bits

  public static int hashInt(int k1) {
    k1 *= C1;
    k1 = Integer.rotateLeft(k1, 15);
    k1 *= C2;
    
    int h1 = seed ^ k1;
    h1 = Integer.rotateLeft(h1, 13);
    h1 = h1 * 5 + 0xe6546b64;
    
    h1 ^= h1 >>> 16;  // unsigned right shift
    h1 *= 0x85ebca6b;
    h1 ^= h1 >>> 13;
    h1 *= 0xc2b2ae35;
    h1 ^= h1 >>> 16;
    return h1;
  }

  public static <T> void setWithExpand(List<T> a, int i, T value) {
    while (i >= a.size()) {
      a.add(null);
    }
    a.set(i, value);
  }
  
  public static boolean equalStrings(String s1, String s2) {
    if (null == s1) {
      return (null == s2);
    }
    return (null == s2) ? false : s1.equals(s2);
  }

  public static int compareStrings(String s1, String s2) {
    if (null == s1) {
      if (null == s2) {
        return 0;
      }
      return -1;
    }
    return (null == s2) ? 1 : s1.compareTo(s2);
  }
  
  public static String elideString(String s, int len) {
    if (s.length() <= len) {
      return s;
    }
    
    return (s.substring(0, len) + "...");
  }

  /**
   * Some objects can be shared, if "equal", rather than creating duplicates, if they're read-only.
   * This may in general be beneficial by reducing the size of the "working set" via more sharing of read-only objects.
   * Users should insure the read-only property.
   * This routine allows 
   *   a) creating a potentially sharable object
   *   b) checking to see if we already have an "equal" one, and 
   *   c) if so, using that and allowing the just created one to be GC'd.
   *   
   * Items in this "set" are held with weak references, so may be gc'd if no longer referenced anywhere.
   * @param obj - the object to use a cached substitute for, if one exists
   * @param cache - the cache
   * @return - the object or a cached version of it.
   */
  public static <T> T shareExisting(T obj, WeakHashMap<T, WeakReference<T>> cache) {
    if (null == obj) {
      throw new IllegalArgumentException();
    }
    T v;
    synchronized (cache) {
      WeakReference<T> r = cache.get(obj);
      if (r == null || (v = r.get()) == null) {
        cache.put(obj, new WeakReference<T>(obj));
        return obj;
      }
      return v;      
    }
  }
  
  /**
   * format a list of items for pretty printing as [item1, item2, ... ]
   * @param items to print
   * @return  [item1, item2, ... ]
   */
  public static String ppList(List<?> items) {
    StringBuilder sb = new StringBuilder(items.size() * 5 + 2);
    return addElementsToStringBuilder(sb, items).toString();
  }

  /**
   * Convert a UIMA type name to a JCas class name (fully qualified)
   *   Normally this is the same, but for two prefixes, it's slightly different
   * @param typeName the UIMA type name, fully qualified
   * @return the fully qualified JCas class name 
   */
  public static String typeName2ClassName(String typeName) {
    if (typeName.startsWith(CAS.UIMA_CAS_PREFIX)) {
      return "org.apache.uima.jcas.cas." + typeName.substring(CAS.UIMA_CAS_PREFIX.length());
    }
    if (typeName.startsWith(CAS.UIMA_TCAS_PREFIX)) {
      return "org.apache.uima.jcas.tcas." + typeName.substring(CAS.UIMA_TCAS_PREFIX.length());
    }
    return typeName;
  }
  
  /**
   * Convert a JCas class name (fully qualified) to a UIMA type name 
   *   Normally this is the same, but for two prefixes, it's slightly different
   * @param typeName the UIMA type name, fully qualified
   * @return the fully qualified JCas class name 
   */
  public static String javaClassName2UimaTypeName(String className) {
    if (className.startsWith("org.apache.uima.jcas.cas.")) { 
      return CAS.UIMA_CAS_PREFIX + className.substring("org.apache.uima.jcas.cas.".length());
    }
    if (className.startsWith("org.apache.uima.jcas.tcas.")) { 
      return CAS.UIMA_TCAS_PREFIX + className.substring("org.apache.uima.jcas.tcas.".length());
    }
    return className;
  }
  
  public static void timeLoops(String title, int iterations, Runnable_withException r) throws Exception {
    long shortest = Long.MAX_VALUE;
    for (int i = 0; i < iterations; i++) {
      long startTime = System.nanoTime();
      r.run();
      long time = (System.nanoTime() - startTime)/ 1000;  // microseconds
      if (time < shortest) {
        shortest = time;
        System.out.format("%s: speed is %,d microseconds on iteration %,d%n", title, shortest, i);
      }
    }
  }
  
  public static void sleep(int milliseconds) {
    try {
      Thread.sleep(milliseconds);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

//private static final Function<String, Class> uimaSystemFindLoadedClass;
//static {
//  try {
//    MethodHandle mh = Misc.getProtectedMethodHandle(JCasImpl.class.getClassLoader().getClass(), "findLoadedClass", String.class);
//    uimaSystemFindLoadedClass = (Function<String, Class>)LambdaMetafactory.metafactory(
//        lookup, // lookup context for the constructor 
//        "apply", // name of the method in the Function Interface 
//        methodType(Function.class),    // signature of callsite, return type is functional interface, args are captured args if any 
//        MethodType.genericMethodType(1), // samMethodType signature and return type of method impl by function object 
//        mh,  // method handle to call
//        methodType(Class.class, String.class)).getTarget().invokeExact();
//  } catch (Throwable e) {
//    throw new UIMARuntimeException(UIMARuntimeException.INTERNAL_ERROR, e);
//  }
//}

//  public static void main(String[] args) {
//    foo();
//        //    String s = "0123456789abcdef0123456789abcde";
//    for (int i = 3; i < 35; i++) {
//      System.out.print(Integer.toString(i) + " " + elide(s, i));
//      System.out.println("|");
//    }
//  }
//  private static  void foo() {
//    System.out.format(" callers %n%s%ndone%n", getCallers(0, 2));
//    
//  }
//    System.out.println("should be false - not defined: " + getNoValueSystemProperty("foo"));
//    System.setProperty("foo", "");
//    System.out.println("should be true - defined, 0 len str value: " + getNoValueSystemProperty("foo"));
//    System.setProperty("foo", "true");
//    System.out.println("should be true - defined, true value: " + getNoValueSystemProperty("foo"));
//    System.setProperty("foo", "zzz");
//    System.out.println("should be true - defined, zzz value: " + getNoValueSystemProperty("foo"));
//    System.setProperty("foo", "false");
//    System.out.println("should be false - defined, false value: " + getNoValueSystemProperty("foo"));
//  }
}