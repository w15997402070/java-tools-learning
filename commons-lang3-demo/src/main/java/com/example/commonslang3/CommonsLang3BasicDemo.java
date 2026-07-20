package com.example.commonslang3;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.math.NumberUtils;

public class CommonsLang3BasicDemo {

    public static void main(String[] args) {
        System.out.println("========== 1. StringUtils 字符串工具 ==========");
        demoStringUtils();

        System.out.println("
========== 2. NumberUtils 数字工具 ==========");
        demoNumberUtils();

        System.out.println("
========== 3. ArrayUtils 数组工具 ==========");
        demoArrayUtils();

        System.out.println("
========== 4. BooleanUtils 布尔工具 ==========");
        demoBooleanUtils();

        System.out.println("
========== 5. CharUtils 字符工具 ==========");
        demoCharUtils();
    }

    private static void demoStringUtils() {
        System.out.println("isEmpty(null)     = " + StringUtils.isEmpty(null));
        System.out.println("isEmpty(String)   = " + StringUtils.isEmpty(""));
        System.out.println("isBlank(String)   = " + StringUtils.isBlank(" "));
        System.out.println("defaultIfBlank    = " + StringUtils.defaultIfBlank(null, "default"));

        String text = "Hello World";
        System.out.println("reverse           = " + StringUtils.reverse(text));
        System.out.println("leftPad(8,'0')   = " + StringUtils.leftPad("42", 8, '0'));
        System.out.println("center(10,'*')   = " + StringUtils.center("OK", 10, '*'));
        System.out.println("repeat('-',20)    = " + StringUtils.repeat('-', 20));

        String[] parts = StringUtils.split("a,b,,c", ',');
        System.out.println("split length      = " + parts.length);
        System.out.println("join              = " + StringUtils.join(parts, "|"));
    }

    private static void demoNumberUtils() {
        System.out.println("toInt(123)       = " + NumberUtils.toInt("123"));
        System.out.println("toInt(abc)       = " + NumberUtils.toInt("abc"));
        System.out.println("toInt(abc,-1)    = " + NumberUtils.toInt("abc", -1));
        System.out.println("toDouble(3.14)   = " + NumberUtils.toDouble("3.14"));
        System.out.println("isCreatable(42)   = " + NumberUtils.isCreatable("42"));
        System.out.println("isParsable(3.14) = " + NumberUtils.isParsable("3.14"));
        System.out.println("max(1,5,3,9,2)   = " + NumberUtils.max(1, 5, 3, 9, 2));
        System.out.println("min(1,5,3,9,2)   = " + NumberUtils.min(1, 5, 3, 9, 2));
    }

    private static void demoArrayUtils() {
        int[] arr = {1, 2, 3, 4, 5};
        System.out.println("toString          = " + ArrayUtils.toString(arr));
        int[] reversed = arr.clone();
        ArrayUtils.reverse(reversed);
        System.out.println("reverse           = " + ArrayUtils.toString(reversed));
        System.out.println("indexOf(3)        = " + ArrayUtils.indexOf(arr, 3));
        System.out.println("contains(6)       = " + ArrayUtils.contains(arr, 6));
        int[] added = ArrayUtils.add(arr, 6);
        System.out.println("add(6)            = " + ArrayUtils.toString(added));
        int[] removed = ArrayUtils.remove(arr, 2);
        System.out.println("remove(idx=2)     = " + ArrayUtils.toString(removed));
        System.out.println("isEmpty(null)      = " + ArrayUtils.isEmpty((int[]) null));
    }

    private static void demoBooleanUtils() {
        System.out.println("toBoolean(1)      = " + BooleanUtils.toBoolean(1));
        System.out.println("toBoolean(yes)    = " + BooleanUtils.toBoolean("yes"));
        System.out.println("toBoolean(no)      = " + BooleanUtils.toBoolean("no"));
        System.out.println("toString(true)     = " + BooleanUtils.toString(true, "Y", "N"));
        System.out.println("isTrue(null)      = " + BooleanUtils.isTrue(null));
    }

    private static void demoCharUtils() {
        System.out.println("isAscii('A')      = " + CharUtils.isAscii('A'));
        System.out.println("isAsciiAlpha('Z') = " + CharUtils.isAsciiAlpha('Z'));
        System.out.println("isAsciiNumeric('5')= " + CharUtils.isAsciiNumeric('5'));
    }
}
