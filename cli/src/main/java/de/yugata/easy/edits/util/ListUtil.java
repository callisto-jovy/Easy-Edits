package de.yugata.easy.edits.util;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.RandomAccess;
import java.util.function.BiPredicate;

public class ListUtil {

    public static final Random RANDOM = new Random();


    /**
     * Shuffles a list based on a bi predicate of the index & the random index to shuffle
     *
     * @param list          the list to shuffle
     * @param swapPredicate the {@link BiPredicate} to determine whether a specific index is to be swapped with its random next element
     * @param <T>           generic type
     */
    public static <T> void shuffle(List<T> list, final BiPredicate<T, T> swapPredicate) {

        final int size = list.size();
        if (size >= 5 && !(list instanceof RandomAccess)) {

            for (int i = size; i > 1; --i) {
                final int nextInt = RANDOM.nextInt(i);
                final T f0 = list.get(i - 1);
                final T f1 = list.get(nextInt);

                if (swapPredicate.test(f0, f1)) {
                    Collections.swap(list, i - 1, nextInt);
                }
            }
        } else {
            for (int i = size; i > 1; --i) {
                Collections.swap(list, i - 1, RANDOM.nextInt(i));
            }
        }
    }
}
