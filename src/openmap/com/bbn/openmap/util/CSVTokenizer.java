// **********************************************************************
// 
// <copyright>
// 
//  BBN Technologies, a Verizon Company
//  10 Moulton Street
//  Cambridge, MA 02138
//  (617) 873-8000
// 
//  Copyright (C) BBNT Solutions LLC. All rights reserved.
// 
// </copyright>
// **********************************************************************
// 
// $Source: /cvs/distapps/openmap/src/openmap/com/bbn/openmap/util/CSVTokenizer.java,v $
// $RCSfile: CSVTokenizer.java,v $
// $Revision: 1.3 $
// $Date: 2004/01/26 18:18:15 $
// $Author: dietrick $
// 
// **********************************************************************



package com.bbn.openmap.util;

/**
 * Tokenizer for comma separated values files, at least as generated
 * by excel.
 * <p>
 * token() returns the next token, which can be either:
 * <ul>
 * <li> null, indicating an empty field.
 * <li> a Double, indicating a numeric field.
 * <li> a String, indicating an alphanumeric field.
 * <li> the NEWLINE object, indicating the end of a record.
 * <LI> the EOF object, test with isEOF(), indicating the end of file.
 * </ul>
 * <pre>
 * </pre>
 */
public class CSVTokenizer extends Tokenizer {
    /** A flag the makes the tokenizer read numbers as strings. */
    boolean numberReadAsString = false;

    public CSVTokenizer(java.io.Reader in) {
        super(in); 
    }

    /**
     * If you set numberReadAsString is true, then any number will be
     * maintained as a String.
     */
    public CSVTokenizer(java.io.Reader in, boolean numberReadAsString) {
        super(in); 
        this.numberReadAsString = numberReadAsString;
    }
    
    /**
     * Return the next object read from the stream.
     */
    public Object token() {
        int c = next();
        if (c == ',') 
            return null;
        else if (c == '\n') 
            return NEWLINE;
        else if (c == '"')
            return tokenString(next());
        else if (c == '\\')
            return tokenString(c);
        else if ((c == '-' || c == '.' || isDigit(c)) && !numberReadAsString)
            return tokenNumber(c);
        else if (c == -1)
            return EOF;
        else                       
            return tokenAny(c);
    }

    /**
     * seq(is('"'), many(alt(seq(isNot('"')), bpush)<BR>
     * seq(is('"')),alt(seq(is('"'), bpush))),
     */

    Object tokenString(int c) {
        while (true) {
            // Enable escapes to force characters into string.
            if (c == '\\') {
                bpush(next());
                c = next();
            } else if (c == '"') {
                // Changed from the commented-out code below, 
                // in order to ignore quotes in any order until
                // delimiter is reached.  Quotes preceded by the
                // escape character live on in the string, via the
                // code above.
                c = next();
                if (isDelimiter(c)) return bclear();
                else continue;

//              int c1 = next();
//              if (c1 == '"') {
//                  bpush(c1);
//                  c = next();
//              } else {
//                  if (isDelimiter(c1)) {
//                      return bclear();
//                  } else {
//                      return error("Expected Delimiter after string!");
//                  }
//              }

            } else if (isAny(c)) {
                bpush(c);
                c = next(); 
            } else {
                return bclear(); 
            }
        }
    }
    
    /**
     * This checks for the delimiter at the end of a token.  We assume
     * it can either be a ',' separating the next field, or '\n'
     * indicating the end of a field and the end of a record, so we
     * putback(c).
     * <P>       
     * isDelimiter.set(alt(is(','), is(-1), seq(is('\n'), putback)));
     */
    boolean isDelimiter(int c) {
        if (c == ',' || c == -1) {
            return true;
        } else if (c == '\n') {
            putback(c);         // Wait for next token().
            return true; 
        } else {
            return false; 
        }
    }
    
    /** 
     * Return a number or a string.
     */
    Object tokenNumber(int c) {
        Object result = tokenAny(c);
        try {
            Double d = new Double((String) result);
            return d; 
        } catch (NumberFormatException e) {
            return result; 
        }
    }
    
    /**
     * Return anything up to the next delimiter as a string.
     * tokenAny.set(alt(seq(isDelimiter, bclear), seq(bpush,tokenAny)))
     */
    Object tokenAny(int c) {
        while (true) {
            if (isDelimiter(c)) {
                return bclear();
            } else {
                bpush(c);
                c = next(); 
            }
        }
    }
    
    public static void main(String[] args) {
        try {
            CSVTokenizer csv = new 
                // CSVTokenizer(new java.io.FileReader(args[0]));
                CSVTokenizer(new java.io.BufferedReader
                             (new java.io.FileReader(args[0])));
            // new java.io.InputStreamReader
            //        (new java.io.FileInputStream(args[0]))));
            while(true) {
                Object token = csv.token();
                if (csv.isEOF(token)) return;
                System.out.println(token); 
            }
        } catch(Exception e) {
            System.out.println(e); 
        }
    }
}
