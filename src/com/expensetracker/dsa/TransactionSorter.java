package com.expensetracker.dsa;

import com.expensetracker.model.Transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Demonstrates Java Collections, Comparator, and custom sorting algorithms (MergeSort & QuickSort).
 *
 * NOTE FOR INTERVIEW:
 * For this learning project, some sorting and filtering is implemented in Java to demonstrate
 * Collections and Comparator. In a production application with large datasets, filtering,
 * sorting, and pagination would generally be pushed to the database engine via SQL ORDER BY and LIMIT.
 */
public final class TransactionSorter {

    public static final Comparator<Transaction> BY_DATE_DESC = (a, b) -> {
        int cmp = b.getTransactionDate().compareTo(a.getTransactionDate());
        return (cmp != 0) ? cmp : Integer.compare(b.getTransactionId(), a.getTransactionId());
    };

    public static final Comparator<Transaction> BY_DATE_ASC = (a, b) -> {
        int cmp = a.getTransactionDate().compareTo(b.getTransactionDate());
        return (cmp != 0) ? cmp : Integer.compare(a.getTransactionId(), b.getTransactionId());
    };

    public static final Comparator<Transaction> BY_AMOUNT_DESC = (a, b) -> {
        int cmp = Double.compare(b.getAmount(), a.getAmount());
        return (cmp != 0) ? cmp : BY_DATE_DESC.compare(a, b);
    };

    public static final Comparator<Transaction> BY_AMOUNT_ASC = (a, b) -> {
        int cmp = Double.compare(a.getAmount(), b.getAmount());
        return (cmp != 0) ? cmp : BY_DATE_DESC.compare(a, b);
    };

    public static final Comparator<Transaction> BY_DESCRIPTION_ASC = (a, b) -> {
        int cmp = a.getDescription().compareToIgnoreCase(b.getDescription());
        return (cmp != 0) ? cmp : BY_DATE_DESC.compare(a, b);
    };

    private TransactionSorter() {
    }

    public static Comparator<Transaction> getComparator(String sortField) {
        if (sortField == null) return BY_DATE_DESC;
        String s = sortField.trim().toLowerCase();
        switch (s) {
            case "oldest":
            case "date-asc":
                return BY_DATE_ASC;
            case "highest":
            case "amount-desc":
                return BY_AMOUNT_DESC;
            case "lowest":
            case "amount-asc":
                return BY_AMOUNT_ASC;
            case "description":
                return BY_DESCRIPTION_ASC;
            case "newest":
            case "date-desc":
            default:
                return BY_DATE_DESC;
        }
    }

    public static List<Transaction> sort(List<Transaction> list, Comparator<Transaction> comparator) {
        if (list == null || list.size() <= 1) {
            return (list == null) ? new ArrayList<>() : new ArrayList<>(list);
        }
        List<Transaction> copy = new ArrayList<>(list);
        Collections.sort(copy, comparator);
        return copy;
    }

    /**
     * Hand-crafted MergeSort implementation demonstrating Divide-and-Conquer O(N log N) sorting.
     */
    public static void mergeSort(List<Transaction> list, Comparator<Transaction> cmp) {
        if (list == null || list.size() <= 1) return;

        int mid = list.size() / 2;
        List<Transaction> left = new ArrayList<>(list.subList(0, mid));
        List<Transaction> right = new ArrayList<>(list.subList(mid, list.size()));

        mergeSort(left, cmp);
        mergeSort(right, cmp);

        int i = 0, j = 0, k = 0;
        while (i < left.size() && j < right.size()) {
            if (cmp.compare(left.get(i), right.get(j)) <= 0) {
                list.set(k++, left.get(i++));
            } else {
                list.set(k++, right.get(j++));
            }
        }
        while (i < left.size()) list.set(k++, left.get(i++));
        while (j < right.size()) list.set(k++, right.get(j++));
    }

    /**
     * Hand-crafted QuickSort implementation demonstrating In-Place Partitioning.
     */
    public static void quickSort(List<Transaction> list, Comparator<Transaction> cmp, int low, int high) {
        if (low < high) {
            int mid = low + (high - low) / 2;
            Collections.swap(list, mid, high);
            Transaction pivot = list.get(high);
            int i = low - 1;

            for (int j = low; j < high; j++) {
                if (cmp.compare(list.get(j), pivot) <= 0) {
                    i++;
                    Collections.swap(list, i, j);
                }
            }
            Collections.swap(list, i + 1, high);
            int pivotIndex = i + 1;

            quickSort(list, cmp, low, pivotIndex - 1);
            quickSort(list, cmp, pivotIndex + 1, high);
        }
    }
}
