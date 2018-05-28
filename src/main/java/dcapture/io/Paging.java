package dcapture.io;

import java.util.List;
import java.util.Objects;

public class Paging {
    private int start, limit;
    private List<String> sortingOrder;
    private String searchText;

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    public List<String> getSortingOrder() {
        return sortingOrder;
    }

    public void setSortingOrder(List<String> sortingOrder) {
        this.sortingOrder = sortingOrder;
    }

    public boolean isSearchText() {
        return searchText != null && !searchText.trim().isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Paging)) return false;
        Paging paging = (Paging) o;
        return Objects.equals(toString(), paging.toString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getStart(), getLimit(), getSortingOrder(), getSearchText());
    }

    @Override
    public String toString() {
        return "{" + "start=" + start + ", limit=" + limit + ", sortList=" + sortingOrder
                + ", searchText='" + searchText + '\'' + '}';
    }
}
