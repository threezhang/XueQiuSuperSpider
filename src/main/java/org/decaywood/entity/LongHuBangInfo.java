package org.decaywood.entity;

import org.decaywood.utils.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author: decaywood
 * @date: 2015/11/27 0:29
 */
public class LongHuBangInfo implements DeepCopy<LongHuBangInfo> {

    private final Stock stock;
    private final Date date;

    private final Set<BizsunitInfo> topBuyList;
    private final Set<BizsunitInfo> topSaleList;


    class ComparatorBuy implements Comparator {
        public int compare(Object arg0, Object arg1) {
            BizsunitInfo biz0 = (BizsunitInfo) arg0;
            BizsunitInfo biz1 = (BizsunitInfo) arg1;
            return Double.valueOf(biz1.getBuyamt()).compareTo(Double.valueOf(biz0.getBuyamt()));
        }
    }

    class ComparatorSale implements Comparator {
        public int compare(Object arg0, Object arg1) {
            BizsunitInfo biz0 = (BizsunitInfo) arg0;
            BizsunitInfo biz1 = (BizsunitInfo) arg1;
            return Double.valueOf(biz1.getSaleamt()).compareTo(Double.valueOf(biz0.getSaleamt()));
        }
    }


    public static class BizsunitInfo implements Serializable {
        private final String bizsunitcode; //营业部编号
        private final String bizsunitname; //营业部名称
        private final String buyamt; // 买入额度
        private final String saleamt; // 卖出额度
        private final String tradedate; //交易日期 yyyymmdd

        public BizsunitInfo(String bizsunitcode, String bizsunitname, String buyamt, String saleamt, String tradedate) {

            if(StringUtils.nullOrEmpty(bizsunitcode, bizsunitname, buyamt, saleamt, tradedate))
                throw new IllegalArgumentException();

            this.bizsunitcode = bizsunitcode;
            this.bizsunitname = bizsunitname;
            this.buyamt = buyamt;
            this.saleamt = saleamt;
            this.tradedate = tradedate;
        }

        public String getBizsunitcode() {
            return bizsunitcode;
        }

        public String getBizsunitname() {
            return bizsunitname;
        }

        public String getBuyamt() {
            return buyamt;
        }

        public String getSaleamt() {
            return saleamt;
        }

        public String getTradedate() {
            return tradedate;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            BizsunitInfo info = (BizsunitInfo) o;

            return bizsunitname.equals(info.bizsunitname);

        }

        @Override
        public int hashCode() {
            return bizsunitname.hashCode();
        }


    }

    public LongHuBangInfo(Stock stock, Date date, Set<BizsunitInfo> topBuyList, Set<BizsunitInfo> topSaleList) {
        this.stock = stock;
        this.date = date;
        this.topBuyList = topBuyList;
        this.topSaleList = topSaleList;
    }



    public Stock getStock() {
        return stock;
    }

    public Date getDate() {
        return date;
    }

    //龙虎榜买入是否有该营业部出现
    public boolean bizsunitInBuyList(String name) {
        return bizsunitInBuyList(name, false);
    }

    //龙虎榜买入是否有该营业部出现
    public boolean bizsunitInBuyList(String name, boolean partlySearch) {
        if (partlySearch) {
            contains(topBuyList, name);
        }
        return topBuyList.contains(new BizsunitInfo("xx", name, "xx", "xx", "xx"));
    }


    //龙虎榜卖出是否有该营业部出现
    public boolean bizsunitInSaleList(String name) {
        return bizsunitInSaleList(name, false);
    }

    //龙虎榜卖出是否有该营业部出现
    public boolean bizsunitInSaleList(String name, boolean partlySearch) {
        if (partlySearch) {
           contains(topSaleList, name);
        }
        return topSaleList.contains(new BizsunitInfo("xx", name, "xx", "xx", "xx"));
    }

    private boolean contains(Set<BizsunitInfo> set, String name) {
        for (BizsunitInfo info : set) {
            if (info.getBizsunitname().contains(name)) return true;
        }
        return false;
    }

    public Set<BizsunitInfo> getTopBuyList() {
        return topBuyList;
    }

    public List<BizsunitInfo> getSortTopBuyList() {
        List<BizsunitInfo> list = new ArrayList<>();
        ComparatorBuy comparator = new ComparatorBuy();
        list.addAll(topBuyList);
        Collections.sort(list, comparator);

//        if(list.size() > 5) {
//            List<BizsunitInfo> newList = new ArrayList<>();
//            newList.add(list.get(0));
//            newList.add(list.get(1));
//            newList.add(list.get(2));
//            newList.add(list.get(3));
//            newList.add(list.get(4));
//            return newList;
//        }
        return list;
    }

    public Set<BizsunitInfo> getTopSaleList() {
        return topSaleList;
    }

    public List<BizsunitInfo> getSortTopSaleList() {
        List<BizsunitInfo> list = new ArrayList<>();
        ComparatorSale comparator = new ComparatorSale();
        list.addAll(topSaleList);
        Collections.sort(list, comparator);
//        if(list.size() > 5) {
//            List<BizsunitInfo> newList = new ArrayList<>();
//            newList.add(list.get(0));
//            newList.add(list.get(1));
//            newList.add(list.get(2));
//            newList.add(list.get(3));
//            newList.add(list.get(4));
//            return newList;
//        }
        return list;
    }

    @Override
    public LongHuBangInfo copy() {
        return new LongHuBangInfo(stock.copy(), date, new HashSet<>(topBuyList), new HashSet<>(topBuyList));
    }
}
