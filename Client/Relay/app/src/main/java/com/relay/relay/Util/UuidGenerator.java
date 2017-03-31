package com.relay.relay.Util;

import android.util.Log;

import java.util.UUID;

/**
 * Created by omer on 27/03/2017.
 *
 * this class convert email to an uuid and converted it back
 *
 *
 */

public class UuidGenerator {

    private final String TAG = "RELAY_DEBUG: "+ UuidGenerator.class.getSimpleName();
    private final String COMRESSED = "1";
    private final String NOT_COMRESSED = "2";
    private static final String DOMAIN[] = {
            /* Default domains included */
            "aol.com", "att.net", "comcast.net", "facebook.com", "gmail.com", "gmx.com", "googlemail.com",
            "google.com", "hotmail.com", "hotmail.co.uk", "mac.com", "me.com", "mail.com", "msn.com",
            "live.com", "sbcglobal.net", "verizon.net", "yahoo.com", "yahoo.co.uk",
            /* Other global domains */
            "email.com", "games.com" /* AOL */, "gmx.net", "hush.com", "hushmail.com", "icloud.com", "inbox.com",
            "lavabit.com", "love.com" /* AOL */, "outlook.com", "pobox.com", "rocketmail.com" /* Yahoo */,
            "safe-mail.net", "wow.com" /* AOL */, "ygm.com" /* AOL */, "ymail.com" /* Yahoo */, "zoho.com", "fastmail.fm",
            "yandex.com",
            /* United States ISP domains */
            "bellsouth.net", "charter.net", "comcast.net", "cox.net", "earthlink.net", "juno.com",
            /* British ISP domains */
            "btinternet.com", "virginmedia.com", "blueyonder.co.uk", "freeserve.co.uk", "live.co.uk",
            "ntlworld.com", "o2.co.uk", "orange.net", "sky.com", "talktalk.co.uk", "tiscali.co.uk",
            "virgin.net", "wanadoo.co.uk", "bt.com",
            /* Domains used in israel */
            "walla.com", "walla.co.il", "hotmail.co.il",
            /* Domains used in Asia */
            "sina.com", "qq.com", "naver.com", "hanmail.net", "daum.net", "nate.com", "yahoo.co.jp", "yahoo.co.kr", "yahoo.co.id", "yahoo.co.in", "yahoo.com.sg", "yahoo.com.ph",
            /* French ISP domains */
            "hotmail.fr", "live.fr", "laposte.net", "yahoo.fr", "wanadoo.fr", "orange.fr", "gmx.fr", "sfr.fr", "neuf.fr", "free.fr",
            /* German ISP domains */
            "gmx.de", "hotmail.de", "live.de", "online.de", "t-online.de" /* T-Mobile */, "web.de", "yahoo.de",
            /* Russian ISP domains */
            "mail.ru", "rambler.ru", "yandex.ru", "ya.ru", "list.ru",
            /* Belgian ISP domains */
            "hotmail.be", "live.be", "skynet.be", "voo.be", "tvcablenet.be", "telenet.be",
            /* Argentinian ISP domains */
            "hotmail.com.ar", "live.com.ar", "yahoo.com.ar", "fibertel.com.ar", "speedy.com.ar", "arnet.com.ar",
            /* Domains used in Mexico */
            "hotmail.com", "gmail.com", "yahoo.com.mx", "live.com.mx", "yahoo.com", "hotmail.es", "live.com", "hotmail.com.mx", "prodigy.net.mx", "msn.com",
            /* Domains used in Brazil */
            "yahoo.com.br", "hotmail.com.br", "outlook.com.br", "uol.com.br", "bol.com.br", "terra.com.br", "ig.com.br", "itelefonica.com.br", "r7.com", "zipmail.com.br", "globo.com", "globomail.com", "oi.com.br"
    };
    private final int  MAXIMUM_CHARS = 27;
    private final int SIZE_OF_UUID = 32;


    public UuidGenerator() {}

    public String GenerateEmailFromUUID(UUID uuid){

        String uuidString = removeDash(uuid);
        String emailUser = deCompressEmailFromUuid(uuidString);
        String domain = deCompressDomainFromUuid(uuidString);

        Log.e(TAG,"the decoded email is : "+emailUser+"@"+domain);
        return emailUser+"@"+domain;
    }

    private String deCompressEmailFromUuid(String uuidString) {
        String actualLength = uuidString.substring(28,30);
        String emailUserHexCompressed = uuidString.substring(1,Integer.valueOf(actualLength));
        String emailHex="";

        // if email not compressed
        if (uuidString.substring(0,1).equals(NOT_COMRESSED))
            return convertHexToString(emailUserHexCompressed);

        // else continue decompress
        String headChar = emailUserHexCompressed.substring(0,1);
        int i =1;
        while( i < emailUserHexCompressed.length()){
            String currentChar = emailUserHexCompressed.substring(i,i+1);

            if (!headChar.equals("3")){
                // the divider should be "0"
                if (!currentChar.equals("0"))
                    emailHex+=headChar+currentChar;
                else{
                    headChar = emailUserHexCompressed.substring(i+1,i+2);
                    i++;
                }
            }
            else{
                // the divider should be "a"
                if (!currentChar.equals("a"))
                    emailHex+=headChar+currentChar;
                else {
                    headChar = emailUserHexCompressed.substring(i + 1, i + 2);
                    i++;
                }
            }
            i++;
        }
        Log.e(TAG,"the decoded hex is: "+emailHex);
        return convertHexToString(emailHex);
    }

    private String deCompressDomainFromUuid(String uuidString) {

        String codeHex = uuidString.substring(30,32);
        Integer codeDec = Integer.parseInt(codeHex, 16);

        return DOMAIN[codeDec];
    }

    /**
     *  Convert an email to an uuid
     *
     * @param email - the email have to be correct otherwise it will throw exception
     * @return
     */
    public UUID GenerateUUIDFromEmail(String email) throws Exception {


        checkMailIsValid(email);

        String actualLength; //2 chars a number between 10 to 99
        String domain; // string, the part of the email from the @ char
        String domainCode; // domain encoded to 2 char
        String emailUser;  // all chars before '@'
        String emailUserHexCompressed;  // emailUser after compress the hex string
        String fillHex; // dynamic - 0 to X chars

        String[] temp = email.split("@");

        emailUser = temp[0];
        domain = temp[1];

        Log.i(TAG,"start compress email "+email);
        Log.i(TAG,"email size without compress is: "+convertStringToHex(email).length());
        emailUserHexCompressed = compressEmailHex(convertStringToHex(emailUser));
        domainCode = getDomainCode(domain);
        Log.i(TAG,"domainCode is: "+domainCode);

        Log.i(TAG,"emailUserHexCompressed  is: "+emailUserHexCompressed);
        Log.i(TAG,"emailUserHexCompressed size is: "+emailUserHexCompressed.length());

        actualLength = String.valueOf(emailUserHexCompressed.length());
        if (actualLength.length() == 1)
            actualLength = "0"+actualLength;

        Log.i(TAG,"actualLength is: "+actualLength);

        fillHex  = createFillHex(emailUserHexCompressed,
                SIZE_OF_UUID- Integer.valueOf(actualLength)-actualLength.length()-domainCode.length());
        Log.i(TAG,"fillHex size is: "+fillHex.length());

        String uuidString = emailUserHexCompressed+fillHex+actualLength+domainCode;
        Log.i(TAG,"uuidString without dash : "+uuidString);


        uuidString = addDash(uuidString);

        Log.e(TAG,"the new UUID is "+uuidString);

        return UUID.fromString(uuidString);
    }


    private void checkMailIsValid(String email) throws Exception {

        String domain; // string, the part of the email from the @ char
        String emailUser;  // all chars before '@'
        String[] temp = email.split("@");

        emailUser = temp[0];
        domain = temp[1];

        if(getDomainCode(domain)== null)
            throw new Exception(new Exception("Email not valid"));

//        if (convertStringToHex(emailUser).length() > MAXIMUM_CHARS)
//            throw new Exception(new Exception("Email not valid"));

    }

    private String getDomainCode(String domain){
        for (int i = 0; i< DOMAIN.length; i++){
            if ( domain.equals(DOMAIN[i]) ){
                String hex = Integer.toHexString(i);
                if (hex.length()==1)
                    return "0"+hex;
                return hex;
            }
        }
        return null;
    }

    /**
     * the method will compress the email in hex if length is bigger then MAX_CHARS
     * if not it wil add the same string with not compressed char in its beginning.
     * else it will start in a compressed char and start the compresses process:
     * The compress idea is to reduce the odd indexes because they repeat themselves
     * the letters  : 0123465789=? -> 31 32 33 34 36 35 37 38 39 30 3d 3f
     * the letters  : abcdefghijklmno -> 61 62 63 64 65 66 67 68 69 6a 6b 6c 6d 6e 6f
     * the letters  : qrstuvwxyz{|}~ -> 71 72 73 74 75 76 77 78 79 7a 7b 7c 7d 7e
     * the letters  : !#$%&'*+-/ -> 21 23 24 25 26 27 2a 2b 2d 2f
     * the letters  : ^_ -> 5e 5f
     * in all of them there's no 0 (except the first row) in odd indexes.
     * there why th char 0 will separate between the odd indexes
     *
     * @param emailHex
     * @return
     */
    private String compressEmailHex (String emailHex) throws Exception {

        String compressedHex = "";
        String headChar;

        // check if emailHex need to be compressed
        if (emailHex.length() <= MAXIMUM_CHARS){
            Log.e(TAG,"not need to compress, the size is: "+emailHex.length());
            return NOT_COMRESSED+emailHex;
        }


        compressedHex = COMRESSED;

        // Go over all chars in the string and try to compress by reduce the odd chars
        // like this  6d 6e 62 6b --> 6de2b
        headChar = emailHex.substring(0,1);
        compressedHex+=headChar;
        int i = 1;
        while( i < emailHex.length() ){

            String currentChar = emailHex.substring(i,i+1);

            // if char index is odd or even
            if ( i%2 == 0){
                if ( !headChar.equals(currentChar)){
                    // if char is 3  put 'a' as a divider otherwise put '0'
                    if (headChar.equals("3"))
                        compressedHex += "a";
                    else
                        compressedHex += "0";

                    // change the headChar
                    headChar = currentChar;
                    compressedHex+=headChar;
                }
            }
            else{
                // add the char to compressed string
                 compressedHex+=currentChar;
            }
            i++;
        }


        Log.e(TAG, "the original hex :"+emailHex);
        Log.e(TAG, "the compressed hex :"+compressedHex);

        if (compressedHex.length()>MAXIMUM_CHARS)
            throw new Exception(new Exception("Email not valid"));
        return compressedHex;
    }


    /**
     * fill the gap if the email string that converted to string hex is less
     * then MAXIMUM_CHARS chars
     * @param hexString
     * @param fillSize
     * @return
     */
    private String createFillHex(String hexString,int fillSize){
        String fillHex = "";
        int i =0 ;
        while(fillSize > 0){
            if (hexString.length() == i)
                i = 0;
            else{
                fillHex = fillHex+hexString.substring(i,i+1);
                fillSize--;
            }
            i++;
        }
        return fillHex;
    }


    private String addDash(String string){

        StringBuilder stringBuilder = new StringBuilder(string);
        stringBuilder.insert(8,"-");
        stringBuilder.insert(13,"-");
        stringBuilder.insert(18,"-");
        stringBuilder.insert(23,"-");
        return stringBuilder.toString();
    }


    private String removeDash(UUID uuid){
        String uuidString = uuid.toString();
        String[] array = uuidString.split("-");
        String uuidWithoutDash = "";

        for(int i = 0; i< array.length;i++)
            uuidWithoutDash += array[i];
        return uuidWithoutDash;
    }

    private String convertStringToHex(String str){

        char[] chars = str.toCharArray();

        StringBuffer hex = new StringBuffer();
        for(int i = 0; i < chars.length; i++){
            hex.append(Integer.toHexString((int)chars[i]));
        }

        return hex.toString();
    }

    private String convertHexToString(String hex){

        StringBuilder sb = new StringBuilder();
        StringBuilder temp = new StringBuilder();

        // split into two characters
        for( int i=0; i<hex.length()-1; i+=2 ){

            //grab the hex in pairs
            String output = hex.substring(i, (i + 2));
            //convert hex to decimal
            int decimal = Integer.parseInt(output, 16);
            //convert the decimal to character
            sb.append((char)decimal);

            temp.append(decimal);
        }
        return sb.toString();
    }
}
