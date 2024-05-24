/*
 * Copyright 2024 jmontch
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package compactrtf;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Deque;



/**
 * 
 * @author Jmontch
 * 
 * This class is a part of a compactRtf library to extract text from a Rtf file content.
 * The library contains this class, the RtfCommand class which contains Rtf commands,
 * and RtfLogger class which is only for debug helps.
 * 
 * This class parses  Rtf source, interprets commands,and write extracted text.
 * The main function is stripSource, which has as parameters a Reader to read source, 
 * and a Writer to write extracted text character by character. It checks if the first characters 
 * are the first characters of a Rtf file "{\rtf", and if no, the return is code NO_RTF.
 * For file of limited size, already in memory or being read in a single bloc,
 * static function stripLimitedSource with source in String as parameter, 
 * does stripSource calling and returns extracted text. More, static function getLastReturnCode
 * furnishes the return code of the last call to stripLimitedSource.
 * 
 * In addition some utility function are made public (Rtf start sequence check, last EOL delete).
 * 
 * Note also that if a problem is detected, probably corrupted data, no Exception are thrown,
 * and class try to continue to extract text. Result may be incorrect,
 * but the return code CORRUPTED_RTF signals that a problem has been detected
 */

public class RtfStripper {
    
    /**
     * return codes
     */
    public static final int RTF_OK=0;
    public static final int CORRUPTED_RTF=1;
    public static final int NO_RTF=2;
    
    private static int lastReturnCode;
    
    /**
     * static function to simply extract text when rtf file content is in memory or 
     * is of limited length to be read entirely and put in a String
     * @param source String containing Rtf source to strip
     * @param returnAnyway if true return text even if no Rtf or corrupted
     * @return extracted text or null if not rtf text and copyIfNotRtf false
     */
    public static String stripLimitedSource(String source, boolean returnAnyway){    
        Writer writer=new CharArrayWriter();
        Reader reader=new StringReader(source);
        RtfStripper stripper=new RtfStripper();
        lastReturnCode= stripper.stripSource(reader,writer,returnAnyway) ;
        if (returnAnyway||(lastReturnCode==RTF_OK)) try {
            writer.flush();
            String result=writer.toString();                   
            return deleteFinalEOL(result);
        } catch (IOException ex) {
            stripper.warning("stripLimitedSource IOException "+ex.getLocalizedMessage());
        }
        return null;
    }
    
    /**
     * return last stripLimitedSource returnCode
     * @return the returnCode
     */
    public static int getLastReturnCode(){
        return lastReturnCode;
    }
    
    /**
     * Check if text ends with EOL and delete it if so
     * As Java String implicitly add EOL ad end of String, this avoids to add extra EOL at end of text
     * @param text text to check
     * @return text without final EOL if found
     */
    public static String deleteFinalEOL(String text){
        if ((text==null)||(!text.endsWith("\n"))) return text;
        return text.substring(0, text.length() - 1);
    }
    
    /**
     * check if source begins with Rtf sequence
     * @param source Rtf source or first block of Rtf source
     * @return true if start with good sequence
     */
    public static boolean checkRtf(String source){
        return (source.startsWith("{\\rtf"));
    }
    
 
    /**
     * internal class to read commands and following parameter, if any,
     * parameter are decimal digits, except for hexa command where they are hexa digits
     * entry start is called after detecting a slash reverse to start command reading
     */
    private class CommandReader{
        private static final int MAX_PARAMETER_LENGTH = 20;
        private static final int MAX_COMMAND_LENGTH = 30;
    
        private final StringBuilder commandText = new StringBuilder();
        private final StringBuilder parameterText = new StringBuilder();
        
        private void start(){
            commandText.setLength(0);
            parameterText.setLength(0);            
            int ch = sourceRead();
            if (ch == -1) return;            
            if (!Character.isLetter(ch)){ // one special char command
                //commandText.append((char) ch);
                if (ch=='\''){
                    hexaRead();
                    return;
                }                
                handleCommand(String.valueOf((char)ch), parameterText);
                return;
            }
            commandText.append((char) ch);// first letter of command
            while (true){
                ch = sourceRead();
                if (ch==-1) return;
                if  (!Character.isLetter(ch)) break; 
                if (commandText.length() <= MAX_COMMAND_LENGTH) commandText.append((char) ch);
            }
            if (ch == '-'){ // skip, no retained command has parameter with -
                ch = sourceRead();
                if (ch == -1) return;
            }
            if (Character.isDigit(ch)){
                parameterText.append((char) ch);
                while (true){
                     ch = sourceRead();
                     if (ch == -1 ) return;
                     if (!Character.isDigit(ch)) break;
                     if (parameterText.length() <= MAX_PARAMETER_LENGTH) parameterText.append((char) ch);
                }              
            }
            String commandName=commandText.toString();
            boolean isUCommand=RtfCommand.isUnicodeCommand(commandName);
            // if not Unicode command, do not skip character in not space
            // if Unicode command skip character if not a letter or \ (may be inserted replacement character or ?)
            boolean doUnread=(((!isUCommand)&&(ch!=' '))||(isUCommand&&Character.isLetter(ch))||(ch=='\\'));
            if (isUCommand) log("Ucommand ch "+ch+" isLetter "+Boolean.toString(Character.isLetter(ch))+" doUnread "+Boolean.toString(doUnread));
            if (doUnread) sourceUnread();
            if ((parameterText.length()>MAX_PARAMETER_LENGTH)||(commandText.length()>MAX_COMMAND_LENGTH))
                warning("readCommand too long command or parameter: " + commandText.toString()+parameterText.toString());
            else handleCommand(commandName, parameterText);
        }
        

        private void hexaRead(){
            while (parameterText.length()<2){
                int ch=sourceRead();
                //log("hexaRead 1 ch "+ch);
                if (ch<0) return;  //End of  file)
                if (ch=='\\') {
                    sourceUnread();
                    break;
                }
                else parameterText.append((char)ch);
            }
            //log("hexaREad 2 parameter "+parameterText.toString());
            if (parameterText.length()>0){
                int b1=parseHexDigit(parameterText.charAt(0));
                int result=b1;
                if (parameterText.length()>1){
                    int b2=parseHexDigit(parameterText.charAt(1));
                    result=16*b1+b2;
                }
                if (result>=0) processCharacter(transcode((byte)result));//((char)result);
                else warning("Hex CheckReading bad Hex b1 "+b1+" 16b1+b2 "+result);
            }
        }
        
        private int parseHexDigit(int digit){
            //log("parseHexDigit digit "+digit);
            if ((digit>=48)&&(digit<=57)) return digit-48; // 0 à 9
            if ((digit>=97)&&(digit<=102)) return digit-87; // a à f 
            if ((digit>=65)&&(digit<=70)) return digit-55; // A à F
            return -1;
        }
        
        private char transcode(byte code){
            if (currentCharset==null) return (char)code;
            byte[] bytes={code};
            String s=new String(bytes,0,1,currentCharset);
            return s.charAt(0);
        }
    }
    
    
    private static final int BUFFER_SIZE=100;
    
    private final CommandReader commandReader=new CommandReader();
    private final Deque<Boolean> destinationStack = new ArrayDeque<>();
    
    private int stringIndex;
    private boolean isForText;
    private Charset currentCharset=null;
    private Reader rdr;
    private Writer wrt;
    private final char[] charBuffer=new char[BUFFER_SIZE];
    private int charCount;
    private int returnCode;
    
 
    /**
     * parse Rtf source to extract text
     * @param rdr Reader to read source
     * @param wrt Writer to write extracted text character by character
     * @param copyIfNotRtf if true and not Rtf source, copy source as extracted
     * @return returnCode RTF_OK if good Rtf text,else  CORRUPTED_RTF or NO_RTF
     */
    public int stripSource(Reader rdr,Writer wrt, boolean copyIfNotRtf){        //try {
        this.rdr=rdr;
        this.wrt=wrt;
        charCount=0;
        isForText=true;
        fillBuffer();
        if ((charCount>6)? checkRtf(new String(charBuffer,0,6)):false) {
            returnCode=RTF_OK;
            parse();
        }
        else {
            returnCode=NO_RTF;
            if (copyIfNotRtf) while (true){
                int ch=sourceRead();
                if (ch<0) break;
                processCharacter((char)ch);
            }
        }
        try {
            rdr.close();
        } catch (IOException ex) {
            warning("stripSource IOException "+ex.getLocalizedMessage());
        }
            return returnCode;
    }
    
    
    private void processCharacter(char c){
        if (isForText) try {
            wrt.write(c);
        } catch (IOException ex) {
            warning("processCharacter IO Exception "+ex.getLocalizedMessage());
        }
       
   }
    
    private boolean fillBuffer(){
        if (charCount>0){
            charBuffer[0]=charBuffer[charCount -1];
            stringIndex=1;
        }
        else stringIndex=0;
        try {
            int count=rdr.read(charBuffer,stringIndex,BUFFER_SIZE-stringIndex);
            charCount=((count>0)? count+stringIndex: -1);
            return (count>0);
        } catch (IOException ex) {
            warning("fillBuffer IOException "+ex.getLocalizedMessage());
            return false;
        }
    }
    
    
    private int sourceRead(){
        //if (charCount<0) return -1;
        if ((charCount<0)||((stringIndex>=charCount)&&(!fillBuffer()))) return -1;
        return charBuffer[stringIndex++];
       
    }
   
    private void sourceUnread(){
        if (stringIndex>0) stringIndex--;
        else warning("sourceUnread stringIndex found<=0 stringIndex "+stringIndex+" buffer count "+charCount);
    }
    
    private void saveDestination(){
        log("saveDestination level "+destinationStack.size()+" isForText "+isForText);
        destinationStack.push(isForText);
    }
    
    private void restoreDestination(){
        if (!destinationStack.isEmpty()) {
            isForText=destinationStack.pop();
            log("restoreDestination level "+destinationStack.size()+" isForText "+Boolean.toString(isForText));
        }
        else warning("restoreDestination call wirh empty stack");
    }
   
    private void parse() {
        while (true){
            int ch = sourceRead();
            if (ch == -1) break; // source end or source bloc end
            switch (ch){
                case '{':
                    saveDestination();
                    break;
                 case '}':
                    restoreDestination();
                    break;
                case '\\':
                    commandReader.start();
                    break;
                case '\r':
                case '\n':
                    break;
                //case '\t':  // do default 
                //    processCommand(RtfCommand.tab, 0, false);
                //    break;
                default:
                    processCharacter((char)ch);
                    break;   
             }
        }
        if (!destinationStack.isEmpty()) warning("parse destinationStack not empty at end size "+destinationStack.size());
    }
    

    
    /**
    * Determine what to do with the extracted command
    * Note that we silently ignore commands that we don't recognise. 
    */
    private void handleCommand(String commandName, StringBuilder parameter){
        RtfCommand command = RtfCommand.getInstance(commandName);
        if (command != null) switch (command.getCommandType()){
            case RtfCommand.TEXT_DEST :
                isForText=true;
                break;
            case RtfCommand.NO_TEXT_DEST :
                isForText=false;
                break;
            case RtfCommand.INSERTION_CHAR :
                processCharacter(command.getInsertionChar());
                break;
            case RtfCommand.UNICODE_COMMAND:
                String param=parameter.toString();
                log("handleCommand u param "+param);
                if ((param.length()>0)&&(param.length()<=8)){
                    int code=Integer.parseInt(param);
                    if (code<0x10000) processCharacter((char)code);
                    else warning("handleCommand u too big code "+code);                   
                }
                else warning("handleCommand u erroneous code size "+param);
                break;
            case RtfCommand.CHARSET:
                Charset newCharset=command.getCharset();
                if (newCharset!=null) currentCharset=newCharset;
                log("handleCommand commandName "+commandName+" CHARSET name "+command.getCharsetName()+" isNull "+Boolean.toString(newCharset==null));
                break;
            case RtfCommand.CHARSET_FROM:
                newCharset=RtfCommand.getCharsetFrom(parameter.toString());
                if (newCharset!=null) currentCharset=newCharset;
                log("handleCommand commandName "+commandName+" CHARSET_FROM number "+parameter.toString()+" isNull "+Boolean.toString(newCharset==null));
                break;
        }
    }
    
    // delete next line if you do not use RtfLogger
    private final RtfLogger LOG=new RtfLogger(this);
    
    private void warning(String msg){
        returnCode=CORRUPTED_RTF;
        // delete next line if you do not use RtfLogger
        LOG.warning(msg);
    }
    
    private void log(String msg){
        // delete next line if you do not use RtfLogger
        LOG.log(msg);
    }
    
}
