package com.expensetracker.dsa;

import com.expensetracker.model.Expense;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Data Structures & Algorithms: Sorting implementations for Expenses.
 * Demonstrates:
 *  - Custom MergeSort algorithm: Stable, Divide-and-Conquer, O(N log N) time complexity
 *  - Custom QuickSort algorithm: In-place partitioning, O(N log N) average time complexity
 *  - Java Comparator interface and lambda expressions for flexible multi-attribute sorting
 */
public final class ExpenseSorter {

    // =========================================================================
    //  PREDEFINED COMPARATORS
    // =========================================================================

    public static final Comparator<Expense> BY_DATE_DESC = (a, b) -> {
        int cmp = b.getDate().compareTo(a.getDate());
        return (cmp != 0) ? cmp : Integer.compare(b.getId(), a.getId());
    };

    public static final Comparator<Expense> BY_DATE_ASC = (a, b) -> {
        int cmp = a.getDate().compareTo(b.getDate());
        return (cmp != 0) ? cmp : Integer.compare(a.getId(), b.getId());
    };

    public static final Comparator<Expense> BY_AMOUNT_DESC = (a, b) -> {
        int cmp = Double.compare(b.getAmount(), a.getAmount());
        return (cmp != 0) ? cmp : BY_DATE_DESC.compare(a, b);
    };

    public static final Comparator<Expense> BY_AMOUNT_ASC = (a, b) -> {
        int cmp = Double.compare(a.getAmount(), b.getAmount());
        return (cmp != 0) ? cmp : BY_DATE_DESC.compare(a, b);
    };

    public static final Comparator<Expense> BY_TITLE_ASC = (a, b) -> {
        int cmp = a.getTitle().compareToIgnoreCase(b.getTitle());
        return (cmp != 0) ? cmp : BY_DATE_DESC.compare(a, b);
    };

    public static final Comparator<Expense> BY_CATEGORY = (a, b) -> {
        int cmp = a.getCategory().name().compareTo(b.getCategory().name());
        return (cmp != 0) ? cmp : BY_DATE_DESC.compare(a, b);
    };

    private ExpenseSorter() {
    }

    /**
     * Resolves the appropriate Comparator given field name and order.
     */
    public static Comparator<Expense> getComparator(String field, String order) {
        boolean desc = "desc".equalsIgnoreCase(order);
        if ("amount".equalsIgnoreCase(field)) {
            return desc ? BY_AMOUNT_DESC : BY_AMOUNT_ASC;
        } else if ("title".equalsIgnoreCase(field)) {
            return desc ? BY_TITLE_ASC.reversed() : BY_TITLE_ASC;
        } else if ("category".equalsIgnoreCase(field)) {
            return desc ? BY_CATEGORY.reversed() : BY_CATEGORY;
        }
        // Default to date
        return desc ? BY_DATE_DESC : BY_DATE_ASC;
    }

    // =========================================================================
    //  SORT DISPATCHER
    // =========================================================================

    public enum Algorithm {
        MERGE_SORT,
        QUICK_SORT,
        TIM_SORT // Java standard Collections.sort()
    }

    public static List<Expense> sort(List<Expense> list, Comparator<Expense> comparator, Algorithm algo) {
        if (list == null || list.size() <= 1) {
            return (list == null) ? new ArrayList<>() : new ArrayList<>(list);
        }
        List<Expense> copy = new ArrayList<>(list);
        switch (algo) {
            case MERGE_SORT:
                mergeSort(copy, comparator);
                break;
            case QUICK_SORT:
                quickSort(copy, comparator, 0, copy.size() - 1);
                break;
            case TIM_SORT:
            default:
                Collections.sort(copy, comparator);
                break;
        }
        return copy;
    }

    // =========================================================================
    //  HAND-CRAFTED MERGE SORT: O(N log N) Time, Stable Sort
    // =========================================================================

    /**
     * MergeSort implementation. Recursively divides array into two halves,
     * sorts them, and merges them back in sorted order.
     */
    public static void mergeSort(List<Expense> list, Comparator<Expense> cmp) {
        if (list.size() <= 1) return;

        int mid = list.size() / 2;
        List<Expense> left = new ArrayList<>(list.subList(0, mid));
        List<Expense> right = new ArrayList<>(list.subList(mid, list.size()));

        mergeSort(left, cmp);
        mergeSort(right, cmp);

        merge(list, left, right, cmp);
    }

    private static void merge(List<Expense> list, List<Expense> left, List<Expense> right, Comparator<Expense> cmp) {
        int i = 0, j = 0, k = 0;
        while (i < left.size() && j < right.size()) {
            if (cmp.compare(left.get(i), right.get(j)) <= 0) {
                list.set(k++, left.get(i++));
            } else {
                list.set(k++, right.get(j++));
            }
        }
        while (i < left.size()) {
            list.set(k++, left.get(i++));
        }
        while (j < right.size()) {
            list.set(k++, right.get(j++));
        }
    }

    // =========================================================================
    //  HAND-CRAFTED QUICK SORT: O(N log N) Average Time, In-Place Partitioning
    // =========================================================================

    /**
     * QuickSort implementation using Lomuto partition scheme with middle pivot.
     */
    public static void quickSort(List<Expense> list, Comparator<Expense> cmp, int low, int high) {
        if (low < high) {
            int pivotIndex = partition(list, cmp, low, high);
            quickSort(list, cmp, low, pivotIndex - 1);
            quickSort(list, cmp, pivotIndex + 1, high);
        }
    }

    private static int partition(List<Expense> list, Comparator<Expense> cmp, int low, int high) {
        // Choose middle element as pivot to mitigate worst-case on already sorted data
        int mid = low + (high - low) / 2;
        Collections.swap(list, mid, high);

        Expense pivot = list.get(high);
        int i = low - 1;

        for (int j = low; j < high; j++) {
            if (cmp.compare(list.get(j), pivot) <= 0) {
                i++;
                Collections.swap(list, i, j);
            }
        }
        Collections.swap(list, i + 1, high);
        return i + 1;
    }
}
