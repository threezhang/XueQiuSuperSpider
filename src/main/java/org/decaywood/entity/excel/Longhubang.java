package org.decaywood.entity.excel;

import java.util.List;
import java.util.Map;

/**
 * @author by jiuru on 2016/11/14.
 */
public class Longhubang {

    private String date;

    private String tradedate; //交易日期 yyyymmdd

    private Map<String, String> stockMap;

    private List<LonghubangDetail> longhubangDetails;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTradedate() {
        return tradedate;
    }

    public void setTradedate(String tradedate) {
        this.tradedate = tradedate;
    }

    public Map<String, String> getStockMap() {
        return stockMap;
    }

    public void setStockMap(Map<String, String> stockMap) {
        this.stockMap = stockMap;
    }

    public List<LonghubangDetail> getLonghubangDetails() {
        return longhubangDetails;
    }

    public void setLonghubangDetails(List<LonghubangDetail> longhubangDetails) {
        this.longhubangDetails = longhubangDetails;
    }
}
