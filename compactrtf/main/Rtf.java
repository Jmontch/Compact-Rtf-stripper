
package main;

import compactrtf.RtfLogger;
import compactrtf.RtfStripper;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.logging.Level;

/**
 *
 * @author jmontch
 * 
 * This main class is to show how using CompactRtfStripper
 * It supposes that are present in execution directory a short rtf file characters.rtf
 * and a larger rtf file testfile.rtf
 */

public class Rtf {


    private static final RtfLogger LOG=new RtfLogger("Rtf");
    private static final String SEPARATOR="************************************************************************";
    

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        LOG.setLevel(Level.INFO);
        stripTest1();
        stripTest2();
        stripTest3();
    }
    
    /*
    * call sreipLimitedSource with Srteing already in memory
    */
    private static void stripTest1(){
        String source="{\\rtf1\\ansi\\ansicpg1252\\deff0\\nouicompat\\deflang1036{\\fonttbl{\\f0\\fnil\\fcharset0 Calibri;}{\\f1\\fnil\\fcharset238 Calibri;}{\\f2\\froman\\fprq2\\fcharset0 Times New Roman;}{\\f3\\fnil\\fcharset161 Calibri;}}\n" +
"{\\*\\generator Riched20 10.0.19041}\\viewkind4\\uc1 \n" +
"\\pard\\nowidctlpar\\f0\\fs22\\lang12 French langage has accentued letters such as \\{\\'e0, \\'e9, \\'e8, \\'eb, \\'ea, \\'ef, \\'ee, \\'f9\\} and combined o and e as in \"\\f1\\u339?uf\".\\f0\\lang1036  I repeat \\f2\\fs24 \"\\'9cuf\". \\par\n" +
"And with inserted digit \\f0\\fs22\\lang12 \"\\f1\\u339?\\f0\\lang1036 9\\f1\\lang12 uf\".\\f0\\lang1036  I repeat \\f2\\fs24 \"\\'9c9uf\". \\par\n" +
"And with inserted ; \\f0\\fs22\\lang12 \"\\f1\\u339?\\f0\\lang1036 ;\\f1\\lang12 uf\".\\f0\\lang1036  I repeat \\f2\\fs24 \"\\'9c;uf\". \\lang1023\\par\n" +
"\\f1\\fs22\\lang12\\par\n" +
"\n" +
"\\pard\\sa200\\sl276\\slmult1 European money is noted \\f0\\'80.\\par\n" +
"Mathematicians like to use some greek letters such as \\f3\\lang1032\\'e1 (alpha) \\f0\\lang1036 ,\\f3\\lang1032  \\'e3 (gamma) \\f0\\lang1036  or \\'b5 (mu)\\f3\\lang1032 ... \\f0\\lang1036\\par\n" +
"}";
        
        LOG.info("stripTest1 limited file already in memory");
        LOG.info(SEPARATOR);
        String text=RtfStripper.stripLimitedSource(source,true);
        LOG.info("stripTest1 returnCode (0 if OK) "+RtfStripper.getLastReturnCode());
        LOG.info("stripTest1 text : \n"+text);
        LOG.info(SEPARATOR);
    }
    
    /*
    * read limited length file in memory then call stripLimitedSource
    */
    private static void stripTest2(){ 
        String filePath="characters.rtf";
        LOG.info("stripTest2 limited file read from path "+filePath);
        int maxLength=1512;
        String source=readAsciiFile(filePath, maxLength);
        if (source!=null){            
            LOG.info(SEPARATOR);
            String text=RtfStripper.stripLimitedSource(source,true);
            LOG.info("stripTest2 returnCode (0 if OK) "+RtfStripper.getLastReturnCode());
            LOG.info("stripTest2 text : \n"+text);  
        }
        else LOG.error("stripTest2 unable to read file "+filePath);
        LOG.info(SEPARATOR);
    }
    
    /*
    * read ASCII file with path filePath and legthMax
    * retiurn in Java String
    */
    private static String readAsciiFile(String filePath, int lengthMax){
        InputStream is = null;
        try {
            File theFile=new File(filePath);
            int length=(int)Math.min(theFile.length(), lengthMax);                       
            is=new FileInputStream(theFile);
            byte[] buffer=new byte[length];
            int count=is.read(buffer);
            LOG.info("readAsciiFile file "+filePath+" file length "+theFile.length()+" length read "+count); 
            return new String(buffer,0,count,"US-ASCII");
        } catch (FileNotFoundException ex) {
            LOG.error("readAsciiFile FILENOTFOUND file "+filePath+" message "+ex.getLocalizedMessage());
        } catch (UnsupportedEncodingException ex) {
            LOG.error("readAsciiFile UNSUPPORTED ENCODING "+ex.getLocalizedMessage());
        } catch (IOException ex) {
                LOG.error("readAsciiFile IO EXCEPTION file "+filePath+" message "+ex.getLocalizedMessage());
        }finally {
            try {
                if (is!=null) is.close();
            } catch (IOException ex) {
                LOG.error("readAsciiFile IO EXCEPTION CLOSING file "+filePath+" message "+ex.getLocalizedMessage());
            }
        }
        return  null;
    }
    
   
    // parse large file using RtfStripper
    private static void stripTest3(){
        String filePath="testfile.rtf";
        Writer writer=null;
        try {
            File theFile=new File(filePath);
            LOG.info("stripTest3 file "+filePath+" file length "+theFile.length());
            LOG.info(SEPARATOR);
            Reader reader = new FileReader(theFile);
            RtfStripper stripper=new RtfStripper();
            writer=new ResultWriter();
            int returnCode=stripper.stripSource(reader, writer, true);
            writer.flush();
            LOG.info(SEPARATOR);
            LOG.info("stripTest3 finished returnCode (0 if OK): "+returnCode);
            LOG.info(SEPARATOR);
        } catch (FileNotFoundException ex) {
            LOG.error("stripTest3 FILENOTFOUND file "+filePath+" message "+ex.getLocalizedMessage());
        } catch (IOException ex) {
            LOG.error("stroipTest3 IO EXCEPTION file "+filePath+" message "+ex.getLocalizedMessage());
        } finally {
            try {
                if (writer!=null) writer.close();
            } catch (IOException ex) {
                LOG.error("stripTest3 IO EXCEPTION closing file "+filePath+" message "+ex.getLocalizedMessage());
            }
        }
    }
    
    //private static final int SIZE_MAX=80;
    private static class ResultWriter extends CharArrayWriter{

        @Override
        public void write(int c) {
            if (c=='\n'){
                LOG.info("TEXT : "+toString());
                reset();
            }
            else super.write(c); //To change body of generated methods, choose Tools | Templates.
        }
        
        @Override
        public void flush() {
            super.flush();
            LOG.info("FINAL TEXT : "+toString());
        }   
    }
    
}
