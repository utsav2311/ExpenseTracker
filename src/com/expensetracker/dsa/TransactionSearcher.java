package com.expensetracker.dsa;

import com.expensetracker.model.Transaction;
import com.expensetracker.model.TransactionType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates search and filter algorithms on Java Collections.
 */
public final class TransactionSearcher {

    private TransactionSearcher() {
    }

    /**
     * Linear Search across description, category name, payment method, and notes.
     * Time Complexity: O(N)
     */
    public static List<Transaction> linearSearch(List<Transaction> transactions, String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>(transactions);
        }
        String q = query.trim().toLowerCase();
        List<Transaction> results = new ArrayList<>();

        for (Transaction t : transactions) {
            boolean matchesDesc = t.getDescription() != null && t.getDescription().toLowerCase().contains(q);
            boolean matchesCategory = t.getCategoryName() != null && t.getCategoryName().toLowerCase().contains(q);
            boolean matchesMethod = t.getPaymentMethod() != null && t.getPaymentMethod().toLowerCase().contains(q);
            boolean matchesNotes = t.getNotes() != null && t.getNotes().toLowerCase().contains(q);
            boolean matchesAmount = String.valueOf(t.getAmount()).contains(q);
            boolean matchesDate = t.getTransactionDate() != null && t.getTransactionDate().toString().contains(q);

            if (matchesDesc || matchesCategory || matchesMethod || matchesNotes || matchesAmount || matchesDate) {
                results.add(t);
            }
        }
        return results;
    }

    /**
     * Multi-criteria filtering by transaction type, category ID, and date range.
     */
    public static List<Transaction> filter(List<Transaction> list,
                                           TransactionType type,
                                           Integer categoryId,
                                           LocalDate startDate,
                                           LocalDate endDate) {
        if (list == null) return new ArrayList<>();
        List<Transaction> filtered = new ArrayList<>();

        for (Transaction t : list) {
            if (type != null && t.getTransactionType() != type) {
                continue;
            }
            if (categoryId != null && categoryId > 0 && t.getCategoryId() != categoryId) {
                continue;
            }
            if (startDate != null && t.getTransactionDate().isBefore(startDate)) {
                continue;
            }
            if (endDate != null && t.getTransactionDate().isAfter(endDate)) {
                continue;
            }
            filtered.add(t);
        }
        return filtered;
    }

    /**
     * Binary Search: finds transactions on an exact date from an ascending date-sorted list.
     * Time Complexity: O(log N + K)
     */
    public static List<Transaction> binarySearchByDate(List<Transaction> sortedAscending, LocalDate targetDate) {
        List<Transaction> matches = new ArrayList<>();
        if (sortedAscending == null || sortedAscending.isEmpty() || targetDate == null) {
            return matches;
        }

        int low = 0;
        int high = sortedAscending.size() - 1;
        int foundIndex = -1;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            LocalDate midDate = sortedAscending.get(mid).getTransactionDate();
            int cmp = midDate.compareTo(targetDate);

            if (cmp == 0) {
                foundIndex = mid;
                break;
            } else if (cmp < 0) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        if (foundIndex != -1) {
            int left = foundIndex;
            while (left >= 0 && sortedAscending.get(left).getTransactionDate().equals(targetDate)) {
                left--;
            }
            int right = foundIndex;
            while (right < sortedAscending.size() && sortedAscending.get(right).getTransactionDate().equals(targetDate)) {
                right++;
            }
            for (int i = left + 1; i < right; i++) {
                matches.add(sortedAscending.get(i));
            }
        }

        return matches;
    }
}
