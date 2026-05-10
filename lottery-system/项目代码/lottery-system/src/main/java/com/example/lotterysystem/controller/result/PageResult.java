package com.example.lotterysystem.controller.result;

import java.io.Serializable;
import java.util.List;

public class PageResult<T> implements Serializable {
    private int total;
    private List<T> records;
    private int currentPage;
    private int pageSize;

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public List<T> getRecords() { return records; }
    public void setRecords(List<T> records) { this.records = records; }
    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
}