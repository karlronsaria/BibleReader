package models;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Andrew
 */
public class Names
{
    public static HashMap<String, Integer> getInstallments()
    {
        HashMap<String, Integer> installments = new HashMap<>();
        
        installments.put("1", 1);
        installments.put("1st", 1);
        installments.put("2", 2);
        installments.put("2nd", 2);
        installments.put("3", 3);
        installments.put("3rd", 3);
        installments.put("i", 1);
        installments.put("ii", 2);
        installments.put("iii", 3);
        installments.put("first", 1);
        installments.put("second", 2);
        installments.put("third", 3);
        
        return installments;
    }
    
    public static TreeSet<String> getBookNames()
    {
        TreeSet<String> bookTitles = new TreeSet<>();
        
        try
        {
            java.sql.ResultSet r = DatabaseConnector.request().createStatement().executeQuery("SELECT Name FROM Book_Names;");
            
            while (!r.isClosed() && r.next())
            {
                bookTitles.add(r.getString("Name"));
            }
        }
        catch (ClassNotFoundException | SQLException ex)
        {
            Logger.getLogger(Names.class.getName()).log(Level.SEVERE, null, ex);
            return new TreeSet<>();
        }
        
        return bookTitles;
    }
}
