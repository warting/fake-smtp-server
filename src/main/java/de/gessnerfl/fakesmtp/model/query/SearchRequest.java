package de.gessnerfl.fakesmtp.model.query;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;

import java.util.Optional;

public class SearchRequest {
    public static SearchRequest of(FilterExpression filter) {
        return SearchRequest.of(filter, DEFAULT_PAGE, DEFAULT_PAGE_SIZE, null);
    }

    public static SearchRequest of(FilterExpression filter, Sorting sort) {
        return SearchRequest.of(filter, DEFAULT_PAGE, DEFAULT_PAGE_SIZE, sort);
    }

    public static SearchRequest of(FilterExpression filter, int page, int size, Sorting sort) {
        final var req = new SearchRequest();
        req.setFilter(filter);
        req.setPage(page);
        req.setSize(size);
        req.setSort(sort);
        return req;
    }

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final String DEFAULT_SORT_PROPERTY = "receivedOn";

    private FilterExpression filter;
    @Min(0)
    private int page = DEFAULT_PAGE;
    @Max(500)
    @Min(10)
    private int size = DEFAULT_PAGE_SIZE;
    private Sorting sort;

    public Optional<FilterExpression> getFilter() {
        return Optional.ofNullable(filter);
    }

    public void setFilter(FilterExpression filter) {
        this.filter = filter;
    }

    public int getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page != null ? page : DEFAULT_PAGE;
    }

    public int getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size != null ? size : DEFAULT_PAGE_SIZE;
    }

    public Optional<Sorting> getSort() {
        return Optional.ofNullable(sort);
    }

    public void setSort(Sorting sort) {
        this.sort = sort;
    }

    @JsonIgnore
    public Pageable getPageable() {
        final var sortOrDefault = getSort().map(this::mapSort).orElseGet(() -> Sort.by(Sort.Direction.DESC, DEFAULT_SORT_PROPERTY));
        return PageRequest.of(page, size, sortOrDefault);
    }

    private Sort mapSort(Sorting sorting) {
        return Sort.by(sorting.getOrders().stream().map(this::mapSortOrder).toList());
    }

    private Sort.Order mapSortOrder(SortOrder o) {
        Assert.hasText(o.getProperty(), "property of sort order is missing");
        return new Sort.Order(o.getDirection() == SortDirection.ASC ? Sort.Direction.ASC : Sort.Direction.DESC, o.getProperty());
    }
}
