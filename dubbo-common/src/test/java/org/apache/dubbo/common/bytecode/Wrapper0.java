package org.apache.dubbo.common.bytecode;

/**
 * Auther: semaxwell
 * Time: 2019-10-31 7:56
 * Wrapper.makeWrapper 生成 org.apache.dubbo.demo.DemoService 的 Wrapper 类
 **/
public class Wrapper0 {

}

//public class Wrapper0 extends Wrapper {
//    // 写死的field
//    public static String[] pns; // property names
//    public static java.util.Map pts; // <property name, property types>
//    public static String[] mns; //method name
//    public static String[] dmns; // declaring method names.
//    public static Class[] mts0;
//
//    public String[] getPropertyNames() {
//        return pns;
//    }
//
//    public boolean hasProperty(String n) {
//        return pts.containsKey(n);
//    }
//
//    public Class getPropertyType(String n) {
//        return (Class) pts.get(n);
//    }
//
//    public String[] getMethodNames() {
//        return mns;
//    }
//
//    public String[] getDeclaredMethodNames() {
//        return dmns;
//    }
//
//    /**
//     * @param o instance
//     * @param n value name
//     * @param v value
//     */
//    // c1
//    public void setPropertyValue(Object o, String n, Object v) {
//        org.apache.dubbo.demo.DemoService w;
//        try {
//            w = ((org.apache.dubbo.demo.DemoService) $1);
//        } catch (Throwable e) {
//            throw new IllegalArgumentException(e);
//        }
//        throw new org.apache.dubbo.common.bytecode.NoSuchPropertyException("Not found property \"" + $2 + "\" field or setter method in class org.apache.dubbo.demo.DemoService.");
//    }
//
//    /**
//     * @param o instance
//     * @param n value name
//     * @return
//     */
//    // c2
//    public Object getPropertyValue(Object o, String n) {
//        org.apache.dubbo.demo.DemoService w;
//        try {
//            w = ((org.apache.dubbo.demo.DemoService) $1);
//        } catch (Throwable e) {
//            throw new IllegalArgumentException(e);
//        }
//        throw new org.apache.dubbo.common.bytecode.NoSuchPropertyException("Not found property \"" + $2 + "\" field or setter method in class org.apache.dubbo.demo.DemoService.");
//    }
//
//    /**
//     * @param o instance
//     * @param n method name
//     * @param p
//     * @param v args
//     * @return
//     * @throws java.lang.reflect.InvocationTargetException
//     */
//    // c3
//    public Object invokeMethod(Object o, String n, Class[] p, Object[] v) throws java.lang.reflect.InvocationTargetException {
//        org.apache.dubbo.demo.DemoService w;
//        try {
//            w = ((org.apache.dubbo.demo.DemoService) o);
//        } catch (Throwable e) {
//            throw new IllegalArgumentException(e);
//        }
//        try {
//            if ("sayHello".equals(n) && v.length == 1 && p[0].getName().equals("java.lang.String")) {
//                return ($w) w.sayHello((java.lang.String) v[0]);
//            }
//            if ("sayHello".equals($2) && $3.length == 2 && $3[0].getName().equals("java.lang.String") && $3[1].getName().equals("java.lang.String")) {
//                w.sayHello((java.lang.String) $4[0], (java.lang.String) $4[1]);
//                return null;
//            }
//            if ("ha".equals($2) && $3.length == 2) {
//                w.ha((java.lang.String) $4[0], (java.lang.String) $4[1]);
//                return null;
//            }
//        } catch (Throwable e) {
//            throw new java.lang.reflect.InvocationTargetException(e);
//        }
//        throw new org.apache.dubbo.common.bytecode.NoSuchMethodException("Not found method \"" + $2 + "\" in class org.apache.dubbo.demo.DemoService.");
//    }
//}
