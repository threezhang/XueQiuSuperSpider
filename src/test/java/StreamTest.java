import entryFirst.UserInfoToDBAcceptor;
import jxl.Workbook;
import jxl.format.Alignment;
import jxl.format.Border;
import jxl.format.BorderLineStyle;
import jxl.format.Colour;
import jxl.format.UnderlineStyle;
import jxl.write.Label;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import mapperTest.TestCaseGenerator;
import org.decaywood.collector.CommissionIndustryCollector;
import org.decaywood.collector.DateRangeCollector;
import org.decaywood.collector.HuShenNewsRefCollector;
import org.decaywood.collector.MostProfitableCubeCollector;
import org.decaywood.collector.StockScopeHotRankCollector;
import org.decaywood.entity.Cube;
import org.decaywood.entity.Entry;
import org.decaywood.entity.Industry;
import org.decaywood.entity.LongHuBangInfo;
import org.decaywood.entity.Stock;
import org.decaywood.entity.trend.StockTrend;
import org.decaywood.filter.PageKeyFilter;
import org.decaywood.mapper.cubeFirst.CubeToCubeWithLastBalancingMapper;
import org.decaywood.mapper.cubeFirst.CubeToCubeWithTrendMapper;
import org.decaywood.mapper.dateFirst.DateToLongHuBangStockMapper;
import org.decaywood.mapper.industryFirst.IndustryToStocksMapper;
import org.decaywood.mapper.stockFirst.StockToLongHuBangMapper;
import org.decaywood.mapper.stockFirst.StockToStockWithAttributeMapper;
import org.decaywood.mapper.stockFirst.StockToStockWithStockTrendMapper;
import org.decaywood.mapper.stockFirst.StockToVIPFollowerCountEntryMapper;
import org.decaywood.utils.MathUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author: decaywood
 * @date: 2015/11/24 14:06
 */
public class StreamTest {


    //一阳穿三线个股
    @Test
    public void yiyinsanyang() throws RemoteException {
        List<Stock> stocks = TestCaseGenerator.generateStocks();

        StockToStockWithAttributeMapper attributeMapper = new StockToStockWithAttributeMapper();
        StockToStockWithStockTrendMapper trendMapper = new StockToStockWithStockTrendMapper();

        Predicate<Entry<String, Stock>> predicate = x -> {

            if (x.getValue().getStockTrend().getHistory().isEmpty()) return false;
            List<StockTrend.TrendBlock> history = x.getValue().getStockTrend().getHistory();
            StockTrend.TrendBlock block = history.get(history.size() - 1);
            double close = Double.parseDouble(block.getClose());
            double open = Double.parseDouble(block.getOpen());
            double ma5 = Double.parseDouble(block.getMa5());
            double ma10 = Double.parseDouble(block.getMa10());
            double ma30 = Double.parseDouble(block.getMa30());

            double max = Math.max(close, open);
            double min = Math.min(close, open);

            return close > open && max >= MathUtils.max(ma5, ma10, ma30) && min <= MathUtils.min(ma5, ma10, ma30);

        };

        stocks.parallelStream()
                .map(x -> new Entry<>(x.getStockName(), attributeMapper.andThen(trendMapper).apply(x)))
                .filter(predicate)
                .map(Entry::getKey)
                .forEach(System.out::println);

    }

    //按关键字过滤页面
    @Test
    public void findNewsUcareAbout() throws RemoteException {
        List<URL> news = new HuShenNewsRefCollector(HuShenNewsRefCollector.Topic.TOTAL, 2).get();
        List<URL> res = news.parallelStream().filter(new PageKeyFilter("万孚生物", false)).collect(Collectors.toList());

        List<URL> regexRes = news.parallelStream().filter(new PageKeyFilter("万孚生物", true)).collect(Collectors.toList());
        for (URL re : regexRes) {
            System.out.println("Regex : " + re);
        }
        for (URL re : res) {
            System.out.println("nonRegex : " + re);
        }
    }


    //创业板股票大V统计 （耗时过长）
/*    @Test
    public void getMarketStockFundTrend() {
        MarketQuotationsRankCollector collector = new MarketQuotationsRankCollector(
                MarketQuotationsRankCollector.StockType.GROWTH_ENTERPRISE_BOARD,
                MarketQuotationsRankCollector.ORDER_BY_VOLUME, 500);
        StockToVIPFollowerCountEntryMapper mapper1 = new StockToVIPFollowerCountEntryMapper(3000, 300);//搜集每个股票的粉丝
        UserInfoToDBAcceptor acceptor = new UserInfoToDBAcceptor();//写入数据库
        collector.get()
                .parallelStream() //并行流
                .map(mapper1)
                .forEach(acceptor);//结果写入数据库
    }*/


    //统计股票5000粉以上大V个数，并以行业分类股票 （耗时过长）
    @Test
    public void getStocksWithVipFollowersCount() throws Exception {
        CommissionIndustryCollector collector = new CommissionIndustryCollector();//搜集所有行业
        IndustryToStocksMapper mapper = new IndustryToStocksMapper();//搜集每个行业所有股票
        StockToVIPFollowerCountEntryMapper mapper1 = new StockToVIPFollowerCountEntryMapper(5000, 300);//搜集每个股票的粉丝
        UserInfoToDBAcceptor acceptor = new UserInfoToDBAcceptor();//写入数据库


        List<Industry> s = collector.get();


        List<Long> longs = new ArrayList<>();

        List<Entry<Stock, Integer>> res = collector.get()
                .parallelStream() //并行流
                .map(mapper)
                .flatMap(Collection::stream)
                .map(mapper1)
                .peek(acceptor)
                .collect(Collectors.toList());
        for (Entry<Stock, Integer> re : res) {
            System.out.println(re.getKey().getStockName() + " -> 5000粉丝以上大V个数  " + re.getValue());
        }
    }

    //最赚钱组合最新持仓以及收益走势、大盘走势
    @Test
    public void MostProfitableCubeDetail() throws RemoteException {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2015, Calendar.OCTOBER, 20);
        Date from = calendar.getTime();
        calendar.set(2015, Calendar.NOVEMBER, 25);
        Date to = calendar.getTime();
        MostProfitableCubeCollector cubeCollector = new MostProfitableCubeCollector(MostProfitableCubeCollector.Market.CN,
                MostProfitableCubeCollector.ORDER_BY.DAILY);
        CubeToCubeWithLastBalancingMapper mapper = null;
        try {
            mapper = new CubeToCubeWithLastBalancingMapper();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        CubeToCubeWithTrendMapper mapper1 = new CubeToCubeWithTrendMapper(from, to);
        List<Cube> cubes = cubeCollector.get().parallelStream().map(mapper.andThen(mapper1)).collect(Collectors.toList());
        for (Cube cube : cubes) {
            System.out.print(cube.getName() + " 总收益: " + cube.getTotal_gain());
            System.out.println(" 最新持仓 " + cube.getRebalancing().getHistory().get(1).toString());
        }
    }


    //获取热股榜股票信息
    @Test
    public void HotRankStockDetail() throws RemoteException {
        StockScopeHotRankCollector collector = new StockScopeHotRankCollector(StockScopeHotRankCollector.Scope.US_WITHIN_24_HOUR);
        StockToStockWithAttributeMapper mapper1 = new StockToStockWithAttributeMapper();
        StockToStockWithStockTrendMapper mapper2 = new StockToStockWithStockTrendMapper();
        List<Stock> stocks = collector.get().parallelStream().map(mapper1.andThen(mapper2)).collect(Collectors.toList());
        for (Stock stock : stocks) {
            System.out.print(stock.getStockName() + " -> ");
            System.out.print(stock.getAmplitude() + " " + stock.getOpen() + " " + stock.getHigh() + " and so on...");
            System.out.println(" trend size: " + stock.getStockTrend().getHistory().size());
        }
    }


    //获得某个行业所有股票的详细信息和历史走势 比如畜牧业
    @Test
    public void IndustryStockDetail() throws RemoteException {

        CommissionIndustryCollector collector = new CommissionIndustryCollector();
        IndustryToStocksMapper mapper = new IndustryToStocksMapper();
        StockToStockWithAttributeMapper mapper1 = new StockToStockWithAttributeMapper();
        StockToStockWithStockTrendMapper mapper2 = new StockToStockWithStockTrendMapper();
        Map<Industry, List<Stock>> res = collector.get()
                .parallelStream()
                .filter(x -> x.getIndustryName().equals("畜牧业"))
                .map(mapper)
                .flatMap(Collection::stream)
                .map(mapper1.andThen(mapper2))
                .collect(Collectors.groupingBy(Stock::getIndustry));

        for (Map.Entry<Industry, List<Stock>> entry : res.entrySet()) {
            for (Stock stock : entry.getValue()) {
                System.out.print(entry.getKey().getIndustryName() + " -> " + stock.getStockName() + " -> ");
                System.out.print(stock.getAmount() + " " + stock.getChange() + " " + stock.getDividend() + " and so on...");
                System.out.println(" trend size: " + stock.getStockTrend().getHistory().size());
            }
        }

    }


    //按行业分类获取所有股票
    @Test
    public void IndustryStockInfo() throws RemoteException {

        CommissionIndustryCollector collector = new CommissionIndustryCollector();
        IndustryToStocksMapper mapper = new IndustryToStocksMapper();
        Map<Industry, List<Stock>> res = collector.get()
                .parallelStream()
                .map(mapper)
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(Stock::getIndustry));

        for (Map.Entry<Industry, List<Stock>> entry : res.entrySet()) {
            for (Stock stock : entry.getValue()) {
                System.out.println(entry.getKey().getIndustryName() + " -> " + stock.getStockName());
            }
        }

    }


    String[] goodSeats = new String[]{"中国中投证券有限责任公司宁波江东北路证券营业部",
            "中国中投证券有限责任公司深圳沙井中心路证券营业部",
            "中国银河证券股份有限公司宁波解放南路证券营业部",
            "中信证券股份有限公司上海溧阳路证券营业部",
            "光大证券股份有限公司深圳金田路证券营业部",
            "中信证券股份有限公司上海淮海中路证券营业部",
            "中信建投证券股份有限公司宜昌解放路证券营业部",
            "招商证券股份有限公司深圳蛇口工业七路证券营业部"};

    String[] badSeats = new String[]{"光大证券股份有限公司佛山季华六路证券营业部",
            "华泰证券股份有限公司成都南一环路第二证券营业部",
            "华泰证券股份有限公司深圳益田路荣超商务中心证券营业部",
            "中国银河证券股份有限公司绍兴证券营业部",
            "中信证券股份有限公司上海古北路证券营业部"};






    //龙虎榜数据
    @Test
    public void LongHuBangTracking() throws RemoteException {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2016, Calendar.DECEMBER, 19);
        Date from = calendar.getTime();
        calendar.set(2016, Calendar.DECEMBER, 19);
        Date to = calendar.getTime();
        DateRangeCollector collector = new DateRangeCollector(from, to);
        DateToLongHuBangStockMapper mapper = new DateToLongHuBangStockMapper();
        StockToLongHuBangMapper mapper1 = new StockToLongHuBangMapper();
        List<LongHuBangInfo> s = collector.get()
                .parallelStream()
                .map(mapper)
                .flatMap(List::stream).map(mapper1)
                .sorted(Comparator.comparing(LongHuBangInfo::getDate))
                .collect(Collectors.toList());

        SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd");
        String file = "/Users/three/git-code/XueQiuSuperSpider/"+dt1.format(from) +"-longhubang.xls";
        WritableWorkbook workbook = null;
        try {
            //创建工作薄
            workbook = Workbook.createWorkbook(new File(file));
            //创建新的一页
            WritableSheet sheet = workbook.createSheet(dt1.format(from) + "-龙虎榜数据", 0);

            //创建表头
            createExcelHeader(from, sheet);

            // 设置字体颜色，可以单独对WritableFont设置setColour(...)
            WritableFont cf4 = new WritableFont(WritableFont.ARIAL, 12);
            cf4.setUnderlineStyle(UnderlineStyle.DOUBLE);
            cf4.setColour(Colour.RED);
            // 设置单元格样式
            WritableCellFormat goodFormat = new WritableCellFormat(cf4);
            goodFormat.setBorder(Border.ALL, BorderLineStyle.DASH_DOT, Colour.RED);
            goodFormat.setBackground(Colour.YELLOW);


            WritableCellFormat badFormat = new WritableCellFormat(cf4);
            badFormat.setBorder(Border.ALL, BorderLineStyle.DASH_DOT, Colour.RED);
            badFormat.setBackground(Colour.LIGHT_GREEN);

            int _index = 0;
            int i = 0;
            for (LongHuBangInfo info : s) {

                Double totalAmount = Double.valueOf(info.getStock().getAmount());

                if(totalAmount > 100000000) {
                    _index++;
                    i++;
                    jxl.write.Number number = new jxl.write.Number(0, i, _index);
                    sheet.addCell(number);
                    Label tmp1 = new Label(1, i, info.getStock().getStockNo());
                    sheet.addCell(tmp1);
                    Label tmp2 = new Label(2, i, info.getStock().getStockName(), goodFormat);
                    sheet.addCell(tmp2);

                    Label tmp3 = new Label(3, i, String.format("%.2f", totalAmount / 10000), goodFormat);
                    sheet.addCell(tmp3);

                    //创建龙虎榜买家
                    int _b = createBuyer(sheet, goodFormat, badFormat, i, info, totalAmount);
                    //创建龙虎榜卖家
                    int _s = createSale(sheet, goodFormat, badFormat, i, info, totalAmount);

                    i = _b > _s ? ++_b : ++_s;
                }
            }

            workbook.write();
            workbook.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {

        } finally {

        }


    }

    private int createSale(WritableSheet sheet, WritableCellFormat goodFormat, WritableCellFormat badFormat, int i, LongHuBangInfo info, Double totalAmount) throws WriteException {
        int _s = i;
        for(LongHuBangInfo.BizsunitInfo saleInfo :  info.getSortTopSaleList()) {
            _s++;
            String saleName = saleInfo.getBizsunitname();

            boolean goodSaleFlag = false;
            boolean badSaleFlag = false;

            for(String str : goodSeats) {
                if(saleName.contains(str)) {
                    goodSaleFlag = true;
                }
            }

            for(String str : badSeats) {
                if(saleName.contains(str)) {
                    badSaleFlag = true;
                }
            }

            Double saleAmount = Double.valueOf(saleInfo.getSaleamt());
            String bNumberText = String.format("%.2f", saleAmount / 10000);
            String saleRateText = String.format("%.2f", (saleAmount / totalAmount) * 100) + "%";

            Label sLabel;
            Label sNumber;
            Label sRate;
            if(goodSaleFlag) {
                sLabel = new Label(7, _s, saleName, goodFormat);
                sNumber = new Label(8, _s, bNumberText, goodFormat);
                sRate = new Label(9, _s, saleRateText, goodFormat);
            }else if(badSaleFlag) {
                sLabel = new Label(7, _s, saleName, badFormat);
                sNumber = new Label(8, _s, bNumberText, badFormat);
                sRate = new Label(9, _s, saleRateText, badFormat);
            }else {
                sLabel = new Label(7, _s, saleName);
                sNumber = new Label(8, _s, bNumberText);
                sRate = new Label(9, _s, saleRateText);
            }

            sheet.addCell(sLabel);
            sheet.addCell(sNumber);
            sheet.addCell(sRate);
        }
        return _s;
    }

    private int createBuyer(WritableSheet sheet, WritableCellFormat goodFormat, WritableCellFormat badFormat, int i, LongHuBangInfo info, Double totalAmount) throws WriteException {
        int _b = i;
        for(LongHuBangInfo.BizsunitInfo buyInfo :  info.getSortTopBuyList()) {
            _b++;
            String buyName = buyInfo.getBizsunitname();
            boolean goodBuyFlag = false;
            boolean badBuyFlag = false;

            for(String str : goodSeats) {
                if(buyName.contains(str)) {
                    goodBuyFlag = true;
                }
            }

            for(String str : badSeats) {
                if(buyName.contains(str)) {
                    badBuyFlag = true;
                }
            }

            Double buyAmount = Double.valueOf(buyInfo.getBuyamt());
            String bNumberText = String.format("%.2f", buyAmount / 10000);
            String buyRateText = String.format("%.2f", (buyAmount / totalAmount) * 100) + "%";

            Label bLabel;
            Label bNumber;
            Label bRate;
            if(goodBuyFlag) {
                bLabel = new Label(4, _b, buyName, goodFormat);
                bNumber = new Label(5, _b, bNumberText, goodFormat);
                bRate = new Label(6, _b, buyRateText, goodFormat);
            }else if(badBuyFlag){
                bLabel = new Label(4, _b, buyName, badFormat);
                bNumber = new Label(5, _b, bNumberText, badFormat);
                bRate = new Label(6, _b, buyRateText, badFormat);
            }else {
                bLabel = new Label(4, _b, buyName);
                bNumber = new Label(5, _b, bNumberText);
                bRate = new Label(6, _b, buyRateText);
            }

            sheet.addCell(bLabel);
            sheet.addCell(bNumber);
            sheet.addCell(bRate);
        }
        return _b;
    }

    private void createExcelHeader(Date from, WritableSheet sheet) throws WriteException {
        jxl.write.DateTime date = new jxl.write.DateTime(0, 0, from);
        sheet.addCell(date);
        Label amount = new Label(3, 0, "成交总额(万)");
        sheet.addCell(amount);
        Label buybiz = new Label(4, 0, "买入营业部");
        sheet.addCell(buybiz);
        Label buy = new Label(5, 0, "买入金额(万)");
        sheet.addCell(buy);
        Label buyRate = new Label(6, 0, "买入占比");
        sheet.addCell(buyRate);
        Label salebiz = new Label(7, 0, "卖出营业部");
        sheet.addCell(salebiz);
        Label sale = new Label(8, 0, "卖出金额(万)");
        sheet.addCell(sale);
        Label saleRate = new Label(9, 0, "卖出占比");
        sheet.addCell(saleRate);
    }

    @Test
    public void OneLongHuBangTracking() throws RemoteException {
        String name = "上海凤凰";

        Map<String, Double> buy = new HashMap<>();
        Map<String, Double> sale = new HashMap<>();
        Calendar calendar = Calendar.getInstance();
        calendar.set(2016, Calendar.DECEMBER, 5);
        Date from = calendar.getTime();
        calendar.set(2016, Calendar.DECEMBER, 19);
        Date to = calendar.getTime();

        Date from1 = sortMap(from, to, name, buy, sale);
        String file = "/Users/three/git-code/XueQiuSuperSpider/"+name +"-longhubang.xls";
        genOneExcel(name, buy, sale, from1, file);

    }

    private void genOneExcel(String name, Map<String, Double> buy, Map<String, Double> sale, Date from, String file) {
        WritableWorkbook workbook = null;
        try {
            // 设置字体颜色，可以单独对WritableFont设置setColour(...)
            WritableFont cf4 = new WritableFont(WritableFont.ARIAL, 12);
            cf4.setUnderlineStyle(UnderlineStyle.DOUBLE);
            cf4.setColour(Colour.RED);
            // 设置单元格样式
            WritableCellFormat goodFormat = new WritableCellFormat(cf4);
            goodFormat.setBorder(Border.ALL, BorderLineStyle.DASH_DOT, Colour.RED);
            goodFormat.setBackground(Colour.YELLOW);


            WritableCellFormat badFormat = new WritableCellFormat(cf4);
            badFormat.setBorder(Border.ALL, BorderLineStyle.DASH_DOT, Colour.RED);
            badFormat.setBackground(Colour.LIGHT_GREEN);


            //创建工作薄
            workbook = Workbook.createWorkbook(new File(file));
            //创建新的一页
            WritableSheet sheet = workbook.createSheet(name + "-龙虎榜数据", 0);

            //创建表头
            jxl.write.DateTime date = new jxl.write.DateTime(0, 0, from);

            sheet.addCell(date);
            Label gzName = new Label(1, 0, "股票");
            sheet.addCell(gzName);
            Label buybiz = new Label(2, 0, "营业部");
            sheet.addCell(buybiz);
            Label buyCell = new Label(3, 0, "买入金额(万)");
            sheet.addCell(buyCell);
            Label saleCell = new Label(4, 0, "卖出金额(万)");
            sheet.addCell(saleCell);
            Label diff = new Label(5, 0, "买入差值");
            sheet.addCell(diff);


            Label tmp1 = new Label(1, 1, name);
            sheet.addCell(tmp1);

            int i = 1;
            for(Map.Entry<String, Double> entry : buy.entrySet()) {
                String bizName = entry.getKey();
                Double amount = entry.getValue();

                Label tmp2;
                if(Arrays.asList(goodSeats).contains(bizName)) {
                    tmp2 = new Label(2, i, bizName, goodFormat);
                }else if(Arrays.asList(badSeats).contains(bizName)) {
                    tmp2 = new Label(2, i, bizName, badFormat);
                }else {
                    tmp2 = new Label(2, i, bizName);
                }
                sheet.addCell(tmp2);

                jxl.write.Number tmp3 = new jxl.write.Number(3, i, amount);
                sheet.addCell(tmp3);

                Double saleAmount = null == sale.get(bizName) ? 0.0 : sale.get(bizName);
                jxl.write.Number tmp4 = new jxl.write.Number(4, i, saleAmount);
                sheet.addCell(tmp4);

                jxl.write.Number tmp5 = new jxl.write.Number(5, i, Double.valueOf(String.format("%.2f", amount - saleAmount)));
                sheet.addCell(tmp5);
                i++;
            }


            workbook.write();
            workbook.close();


        }catch (Exception e) {

        }
    }

    private Date sortMap(Date from, Date to, String name, Map<String, Double> buy, Map<String, Double> sale) throws RemoteException {

        DateRangeCollector collector = new DateRangeCollector(from, to);
        DateToLongHuBangStockMapper mapper = new DateToLongHuBangStockMapper();
        StockToLongHuBangMapper mapper1 = new StockToLongHuBangMapper();
        List<LongHuBangInfo> s = collector.get()
                .parallelStream()
                .map(mapper)
                .flatMap(List::stream).map(mapper1)
                .sorted(Comparator.comparing(LongHuBangInfo::getDate))
                .collect(Collectors.toList());

        for(LongHuBangInfo longHuBangInfo : s) {
            String stockName = longHuBangInfo.getStock().getStockName();
            if(stockName.equals(name)) {
                List<LongHuBangInfo.BizsunitInfo> topBuy = longHuBangInfo.getSortTopBuyList();
                List<LongHuBangInfo.BizsunitInfo> topSale = longHuBangInfo.getSortTopSaleList();

                for(LongHuBangInfo.BizsunitInfo bizsunitInfo : topBuy) {
                    String bizName = bizsunitInfo.getBizsunitname();
                    Double buyAmt = Double.valueOf(String.format("%.2f", Double.valueOf(bizsunitInfo.getBuyamt()) / 10000));

                    if(null == buy.get(bizName)) {
                        buy.put(bizName, buyAmt);
                    }else {
                        buy.put(bizName, buy.get(bizName) + buyAmt);
                    }
                }

                for(LongHuBangInfo.BizsunitInfo bizsunitInfo : topSale) {
                    String bizName = bizsunitInfo.getBizsunitname();
                    Double saleAmt = Double.valueOf(String.format("%.2f", Double.valueOf(bizsunitInfo.getSaleamt()) / 10000));
                    if(null == sale.get(bizName)) {
                        sale.put(bizName, saleAmt);
                    }else {
                        sale.put(bizName, sale.get(bizName) + saleAmt);
                    }
                }
            }
        }

        List<Map.Entry<String, Double>> buyinfos = new ArrayList<Map.Entry<String, Double>>(buy.entrySet());
        // 对HashMap中的key 进行排序
        Collections.sort(buyinfos, new Comparator<Map.Entry<String, Double>>() {
            public int compare(Map.Entry<String, Double> o1,
                               Map.Entry<String, Double> o2) {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });

        List<Map.Entry<String, Double>> saleinfos = new ArrayList<Map.Entry<String, Double>>(sale.entrySet());
        // 对HashMap中的key 进行排序
        Collections.sort(saleinfos, new Comparator<Map.Entry<String, Double>>() {
            public int compare(Map.Entry<String, Double> o1,
                               Map.Entry<String, Double> o2) {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });
        return from;
    }


}
