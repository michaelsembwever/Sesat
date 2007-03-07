/*
 * Copyright (2005-2007-2207) Schibsted Søk AS
 * 
 */
package no.schibstedsok.searchportal.util;
/**
 * @author <a href="mailto:magnus.eklund@schibsted.no">Magnus Eklund</a>
 * @version <tt>$Id$</tt>
 */
public final class PagingDisplayHelper {

    private int pageSize = 10;
    private int maxPages = 10;

    private int numberOfResults;

    private int currentOffset = 0;

    public PagingDisplayHelper(final int numberOfResults) {
        this.numberOfResults = numberOfResults;
    }

    public PagingDisplayHelper(final int numberOfResults, final int pageSize, final int maxPages) {
        this.numberOfResults = numberOfResults;
        this.pageSize = pageSize;
        this.maxPages = maxPages;
    }

    public PagingDisplayHelper(final int pageSize, final int maxPages) {
        this.pageSize = pageSize;
        this.maxPages = maxPages;
    }

    public int getCurrentPage() {
        return currentOffset / pageSize + 1;
    }

    public int getNumberOfPages() {
        return (numberOfResults + pageSize - 1) / pageSize;
    }

    public int getPageSize() {
        return pageSize;
    }

    public final void setPageSize(final int pageSize) {
        this.pageSize = pageSize;
    }
    
    public boolean isFirstPage() {
        return getCurrentPage() == 1;
    }

    public boolean isLastPage() {
        return getCurrentPage() == getNumberOfPages();
    }

    public void setCurrentOffset(final int offset) {
        currentOffset = offset;
    }

    public int getOffsetOfNextPage() {
        return getFirstHitOnPage() + pageSize - 1;
    }

    public int getOffsetOfPreviousPage() {
        return (getCurrentPage() - 2) * pageSize;
    }

    public int getOffsetOfPage(final int page) {
        return (page - 1) * (pageSize);
    }

    public int getFirstVisiblePage() {

        int firstPage = 0;
        int n = (getCurrentOffset()/pageSize);
        if (n > 5)
            if ( ( getNumberOfPages() - getCurrentPage() ) < 5) {
                firstPage = getNumberOfPages() - 9;
                if (firstPage <= 0)
                    firstPage = 1;
            } else
                firstPage = (n - 5 + 1);
        else
            firstPage = 1;

        return firstPage;
    }

    public int getLastVisiblePage() {
        int pageSet = getFirstVisiblePage() + maxPages - 1;

        return getNumberOfPages() < pageSet ? getNumberOfPages() : pageSet;
    }

    public int getMaxPages() {
        return maxPages;
    }

    public int getFirstHitOnPage() {
        return (getCurrentPage() - 1) * pageSize + currentOffset % pageSize + 1;
    }

    public int getLastHitOnPage() {
        return Math.min(numberOfResults, getFirstHitOnPage() + pageSize - 1);
    }

    public void setNumberOfResults(final int numberOfResults) {
        this.numberOfResults = numberOfResults;
    }

    public int getNumberOfResults() {
        return numberOfResults;
    }
    
    public int getCurrentOffset() {
        return currentOffset;
    }
}
