package com.expensetracker.dsa;

import com.expensetracker.model.Expense;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Structures & Algorithms: Searching implementations for Expenses.
 * Demonstrates:
 *  - Linear Search: O(N) multi-field text pattern matching
 *  - Binary Search: O(log N) search on pre-sorted collections by Date and Amount
 *  - Substring & Prefix matching
 */
public final class ExpenseSearcher {

    private ExpenseSearcher() {
    }

    /**
     * Linear Search: checks if keyword matches title, category, payment method, notes, or date.
     * Time Complexity: O(N * M) where N is number of expenses, M is field length.
     */
    public static List<Expense> linearSearch(List<Expense> expenses, String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>(expenses);
        }
        String q = query.trim().toLowerCase();
        List<Expense> results = new ArrayList<>();

        for (Expense e : expenses) {
            boolean matchesTitle = e.getTitle().toLowerCase().contains(q);
            boolean matchesCategory = e.getCategory().name().toLowerCase().contains(q)
                    || e.getCategory().getDisplayName().toLowerCase().contains(q);
            boolean matchesMethod = e.getPaymentMethod().name().toLowerCase().contains(q)
                    || e.getPaymentMethod().getDisplayName().toLowerCase().contains(q);
            boolean matchesNotes = (e.getNotes() != null && e.getNotes().toLowerCase().contains(q));
            boolean matchesDate = e.getDate().toString().contains(q);
            boolean matchesAmount = String.valueOf(e.getAmount()).contains(q);

            if (matchesTitle || matchesCategory || matchesMethod || matchesNotes || matchesDate || matchesAmount) {
                results.add(e);
            }
        }
        return results;
    }

    /**
     * Binary Search: finds all expenses on an exact date from a list pre-sorted in ascending date order.
     * Time Complexity: O(log N + K) where K is number of expenses on that target date.
     */
    public static List<Expense> binarySearchByDate(List<Expense> sortedAscending, LocalDate targetDate) {
        List<Expense> matches = new ArrayList<>();
        if (sortedAscending == null || sortedAscending.isEmpty() || targetDate == null) {
            return matches;
        }

        int low = 0;
        int high = sortedAscending.size() - 1;
        int foundIndex = -1;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            LocalDate midDate = sortedAscending.get(mid).getDate();
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
            // Collect all entries with matching date to the left
            int left = foundIndex;
            while (left >= 0 && sortedAscending.get(left).getDate().equals(targetDate)) {
                left--;
            }
            // Collect all entries with matching date to the right
            int right = foundIndex;
            while (right < sortedAscending.size() && sortedAscending.get(right).getDate().equals(targetDate)) {
                right++;
            }
            for (int i = left + 1; i < right; i++) {
                matches.add(sortedAscending.get(i));
            }
        }

        return matches;
    }

    /**
     * Binary Search: finds expense closest to target amount from an amount-sorted ascending list.
     * Time Complexity: O(log N).
     */
    public static Expense binarySearchClosestAmount(List<Expense> sortedByAmountAsc, double targetAmount) {
        if (sortedByAmountAsc == null || sortedByAmountAsc.isEmpty()) {
            return null;
        }
        int low = 0;
        int high = sortedByAmountAsc.size() - 1;

        if (targetAmount <= sortedByAmountAsc.get(low).getAmount()) {
            return sortedByAmountAsc.get(low);
        }
        if (targetAmount >= sortedByAmountAsc.get(high).getAmount()) {
            return sortedByAmountAsc.get(high);
        }

        while (low <= high) {
            int mid = low + (high - low) / 2;
            double midAmt = sortedByAmountAsc.get(mid).getAmount();
            if (Math.abs(midAmt - targetAmount) < 0.001) {
                return sortedByAmountAsc.get(mid);
            }
            if (midAmt < targetAmount) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        // Return the closer of low and high
        double diffLow = Math.abs(sortedByAmountAsc.get(low).getAmount() - targetAmount);
        double diffHigh = Math.abs(sortedByAmountAsc.get(high).getAmount() - targetAmount);
        return (diffLow < diffHigh) ? sortedByAmountAsc.get(low) : sortedByAmountAsc.get(high);
    }
}
