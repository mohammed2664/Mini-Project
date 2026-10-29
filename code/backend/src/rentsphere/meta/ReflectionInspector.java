package rentsphere.meta;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Live RTTI Reflection Inspector.
 * Uses java.lang.reflect to dynamically inspect domain objects and expose
 * inheritance chains, fields, and method signatures to the web UI workbench.
 * MCA OOT Lab - Assignment 4 (Meta-OOP & Introspection)
 */
public class ReflectionInspector {

    public static class InspectionReport {
        private String className;
        private String packageName;
        private List<String> inheritanceChain = new ArrayList<>();
        private List<String> implementedInterfaces = new ArrayList<>();
        private List<Map<String, String>> declaredFields = new ArrayList<>();
        private List<Map<String, String>> declaredMethods = new ArrayList<>();
        private int totalModifiers;

        public String getClassName() {
            return className;
        }

        public String getPackageName() {
            return packageName;
        }

        public List<String> getInheritanceChain() {
            return inheritanceChain;
        }

        public List<String> getImplementedInterfaces() {
            return implementedInterfaces;
        }

        public List<Map<String, String>> getDeclaredFields() {
            return declaredFields;
        }

        public List<Map<String, String>> getDeclaredMethods() {
            return declaredMethods;
        }

        public int getTotalModifiers() {
            return totalModifiers;
        }
    }

    /**
     * Inspects a live object instance at runtime using Java Reflection RTTI.
     */
    public static InspectionReport inspect(Object instance) {
        if (instance == null) {
            throw new IllegalArgumentException("Cannot inspect null object.");
        }

        InspectionReport report = new InspectionReport();
        Class<?> clazz = instance.getClass();
        report.className = clazz.getSimpleName();
        report.packageName = clazz.getPackageName();
        report.totalModifiers = clazz.getModifiers();

        // 1. Build Inheritance Hierarchy Chain
        Class<?> current = clazz;
        while (current != null) {
            report.inheritanceChain.add(current.getName());
            current = current.getSuperclass();
        }

        // 2. Extract Implemented Interfaces
        for (Class<?> iface : clazz.getInterfaces()) {
            report.implementedInterfaces.add(iface.getSimpleName());
        }

        // 3. Extract Declared Fields
        for (Field field : clazz.getDeclaredFields()) {
            Map<String, String> fieldInfo = new HashMap<>();
            fieldInfo.put("name", field.getName());
            fieldInfo.put("type", field.getType().getSimpleName());
            fieldInfo.put("modifiers", Modifier.toString(field.getModifiers()));

            try {
                field.setAccessible(true);
                Object val = field.get(instance);
                fieldInfo.put("value", (val != null) ? val.toString() : "null");
            } catch (Exception e) {
                fieldInfo.put("value", "<inaccessible>");
            }
            report.declaredFields.add(fieldInfo);
        }

        // 4. Extract Declared Methods
        for (Method method : clazz.getDeclaredMethods()) {
            Map<String, String> methodInfo = new HashMap<>();
            methodInfo.put("name", method.getName());
            methodInfo.put("returnType", method.getReturnType().getSimpleName());
            methodInfo.put("modifiers", Modifier.toString(method.getModifiers()));

            List<String> paramTypes = new ArrayList<>();
            for (Class<?> p : method.getParameterTypes()) {
                paramTypes.add(p.getSimpleName());
            }
            methodInfo.put("parameters", String.join(", ", paramTypes));
            report.declaredMethods.add(methodInfo);
        }

        return report;
    }
}
