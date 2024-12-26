/*
 * This file is part of Zapper, licensed under the MIT License.
 *
 *  Copyright (c) Revxrsal <reflxction.github@gmail.com>
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
package revxrsal.zapper.classloader;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Objects;

final class UnsafeUtil {

    private static final Supplier<Unsafe> Unsafe = Suppliers.memoize(() -> {
        try {
            Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (sun.misc.Unsafe) field.get(null);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    });

    private static final Supplier<Boolean> isJava8 = Suppliers.memoize(() -> {
        String version = System.getProperty("java.version");
        return version.contains("1.8");
    });

    public static Unsafe getUnsafe() {
        return Unsafe.get();
    }

    public static boolean isJava8() {
        return isJava8.get();
    }

    public static <T> T getField(Object instance, String name, Class<?> from) {
        try {
            Unsafe unsafe = Unsafe.get();
            Field field = from.getDeclaredField(name);
            long offset = unsafe.objectFieldOffset(field);
            T value = (T) unsafe.getObject(instance, offset);
            return Objects.requireNonNull(value, "getField(" + name + ") from " + from);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static <T> void setField(Object instance, String name, Class<?> from, T value) {
        try {
            Unsafe unsafe = Unsafe.get();
            Field field = from.getDeclaredField(name);
            long offset = unsafe.objectFieldOffset(field);
            unsafe.putObject(instance, offset, value);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}