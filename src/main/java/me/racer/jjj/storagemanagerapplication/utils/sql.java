package me.racer.jjj.storagemanagerapplication.utils;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.coderion.model.Product;
import pl.coderion.model.ProductResponse;
import pl.coderion.service.OpenFoodFactsWrapper;
import pl.coderion.service.impl.OpenFoodFactsWrapperImpl;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class sql {
    private static final Logger log = LoggerFactory.getLogger(sql.class);
    public static Connection mainsqlcon;
    public static Connection pricesqlcon;

    public static void initsql(String host, String port, String database, String pricedatabase, String user, String password) {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            mainsqlcon = DriverManager.getConnection("jdbc:mariadb://" + host + ":" + port + "/" + database , user, password);
            pricesqlcon = DriverManager.getConnection("jdbc:mariadb://" + host + ":" + port + "/" + pricedatabase , user, password);
            Statement sqlstatment = mainsqlcon.createStatement();
            sqlstatment.execute("CREATE TABLE IF NOT EXISTS stock (itemid INT PRIMARY KEY, EAN CHAR(13), amount INT, expirydate DATE, adddate DATETIME)");
            sqlstatment.execute("CREATE TABLE IF NOT EXISTS productcache (EAN CHAR(13) PRIMARY KEY, name VARCHAR(200), quantity VARCHAR(200), imageurl VARCHAR(200), type ENUM('Beverages','Boissons','food'), location ENUM('Fridge','basement') NULL)");
            sqlstatment.execute("CREATE TABLE IF NOT EXISTS analytics (EAN CHAR(13) PRIMARY KEY, totalamount int, closetoexpiryamount int)");
            sqlstatment.close();
        } catch (SQLException | ClassNotFoundException e) {
            sql.log.error("Verbindung zu SQL nicht möglich!");
            System.exit(1);
        }
    }

    public static void addStock(String ean, int amount, String expirydate) throws SQLException {
        Statement stockstat = mainsqlcon.createStatement();
        ResultSet indexnumber =  stockstat.executeQuery("SELECT * FROM stock");
        indexnumber.absolute(-1);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();

        stockstat.executeUpdate("INSERT INTO stock (itemid, EAN, amount, expirydate, adddate) VALUES (" + indexnumber.getInt("itemid")+1 + ", '" + ean + "', '" + amount + "', '" + expirydate + "', '" + dtf.format(now) + "')");
        stockstat.close();
    }

    public static void removeStock(int itemid) throws SQLException {
        Statement stockstat = mainsqlcon.createStatement();

        stockstat.executeUpdate("DELETE FROM stock WHERE itemid = " + itemid);
        stockstat.close();
    }


    public static String getstock(String orderby) throws SQLException {
        JSONObject stockjson = new JSONObject();
        Statement stockstat = mainsqlcon.createStatement();
        ResultSet userresult =  stockstat.executeQuery("SELECT stock.EAN, stock.itemid, productcache.name, sum(stock.amount), min(stock.expirydate), productcache.quantity, productcache.imageurl FROM stock JOIN productcache ON stock.EAN=productcache.EAN GROUP BY stock.EAN ORDER BY "+ orderby +"");

        //Group by EAN

        userresult.absolute(0);
        while (userresult.next()) {
            JSONObject productjson = new JSONObject();
            productjson.put("itemid", userresult.getString("stock.itemid"));
            productjson.put("name", userresult.getString("productcache.name"));
            productjson.put("amount", userresult.getString("sum(stock.amount)"));
            productjson.put("expirydate", userresult.getString("min(stock.expirydate)"));
            productjson.put("quantity", userresult.getString("productcache.quantity"));
            productjson.put("imageurl", userresult.getString("productcache.imageurl"));
            stockjson.put(userresult.getString("EAN"), productjson);
        }
        //System.out.println(stock);
        /**
         userresult.first();
         System.out.println(userresult.getString(1) + " | " +userresult.getString(2)+ " | " +userresult.getString(3)+ " | " +userresult.getString(4)+ " | " + userresult.getString(5));
         userresult.next();
         System.out.println(userresult.getString(1) + " | " +userresult.getString(2)+ " | " +userresult.getString(3)+ " | " +userresult.getString(4)+ " | " + userresult.getString(5));
         **/

        return stockjson.toJSONString();
    }

    public static String getproductstock(String EAN) throws SQLException {
        JSONObject stockjson = new JSONObject();
        Statement stockstat = mainsqlcon.createStatement();
        ResultSet userresult =  stockstat.executeQuery("SELECT stock.EAN, stock.itemid, productcache.name, stock.amount, stock.expirydate, stock.adddate, productcache.quantity, productcache.imageurl FROM stock JOIN productcache ON stock.EAN=productcache.EAN WHERE stock.EAN = '" + EAN + "'ORDER BY stock.expirydate ASC");

        stockjson.put("EAN", userresult.getString("stock.EAN"));
        stockjson.put("name", userresult.getString("productcache.name"));
        stockjson.put("quantity", userresult.getString("productcache.quantity"));
        stockjson.put("imageurl", userresult.getString("productcache.imageurl"));

        userresult.absolute(0);
        JSONObject productjson = new JSONObject();
        while (userresult.next()) {
            productjson.put(userresult.getString("stock.itemid"), "[" + userresult.getString("stock.amount") + "," + userresult.getString("stock.expirydate") + "," + userresult.getString("stock.adddate") + "]");
        }
        stockjson.put("stock", productjson);
        //System.out.println(stock);
        /**
         userresult.first();
         System.out.println(userresult.getString(1) + " | " +userresult.getString(2)+ " | " +userresult.getString(3)+ " | " +userresult.getString(4)+ " | " + userresult.getString(5));
         userresult.next();
         System.out.println(userresult.getString(1) + " | " +userresult.getString(2)+ " | " +userresult.getString(3)+ " | " +userresult.getString(4)+ " | " + userresult.getString(5));
         **/

        return stockjson.toJSONString();
    }



    public static String loadtocache(String EAN) {



        try {
            Statement cachestat = mainsqlcon.createStatement();

            ResultSet checkifexists = cachestat.executeQuery("SELECT EAN FROM productcache WHERE EAN ='" + EAN +  "'");
            if (checkifexists.getString("EAN").equals(EAN)) {
                return "Success - Product is already cached.";
            }

            OpenFoodFactsWrapper wrapper = new OpenFoodFactsWrapperImpl();
            ProductResponse productResponse = wrapper.fetchProductByCode(EAN);


            Product product = productResponse.getProduct();
            if (product == null) {
                return "Product could not be found.";
            }


            System.out.println(product.getCategories().split(",")[0]);
            cachestat.executeUpdate("INSERT INTO productcache (EAN, name, quantity, imageurl, type, location) VALUES ('" + EAN + "', '" + product.getProductName() + "', '" + product.getQuantity() + "', '" + product.getImageUrl() + "', '" + product.getCategories().split(",")[0] + "', '" + "basement" +  "')");
            cachestat.close();
        } catch (SQLException e) {
            return "There was a problem adding it to the cache-database.";
        }

        return "Success - Product is now cached.";
    }
}
//insert into stock(itemid, EAN, amount, expirydate, adddate) values (1, "5449000000439",2,"2024-09-13","2024-08-16 12:13:14");
//insert into stock(itemid, EAN, amount, expirydate, adddate) values (2, "5449000214911",4,"2024-09-18","2024-08-16 14:13:14");