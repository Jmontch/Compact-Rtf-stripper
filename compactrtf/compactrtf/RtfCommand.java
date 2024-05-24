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

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Jmontch
 * 
 * This class allows interpreting commands found in Rtf source
 * Commands are classed in six types :
 * - INSERTION command to insert a specified character in out text
 * - UNICODE command to insert a character whose unicode code follows the command
 * - TEXT_DESTINATION indicating that text after this command is to insert in out text
 * - NO_TEXT_DESTINATION indicating that text which follows is not to insert
 * - CHARSET defining a standard character set, with selection from 1 to 4
 * - CHARSET_FROM define a character set with name "Cpxxxx", where xxxx is a number that follows the command
 * The class construct a Map (command text, code) for all commands.
 * Code is the character to insert, unicode code, for INSERT command,
 * and  codes (>10000 hexa)indicating type for other (and with index for CHARSET).
 * For destination commands, all known commands are retained.
 * For insertion and unicode commands, only them that have action on out text are retained.
 */
class RtfCommand {
    
    private static final int FIRST_TYPE=0x10000;//'\uFFF0';
    /**
     * commands types 
     */
    static final int NO_TEXT_DEST=FIRST_TYPE+0x10;
    static final int TEXT_DEST=FIRST_TYPE+0x20;
    static final int UNICODE_COMMAND=FIRST_TYPE+0x30;
    static final int INSERTION_CHAR=FIRST_TYPE+0x40;
    static final int CHARSET=FIRST_TYPE+0x50;
    static final int CHARSET_FROM=FIRST_TYPE+0x60;
    
    /**
     * Return an instance of the object
     * @param keyword name of the command
     * @return the object or null if keyword is unknown
     */
    static RtfCommand getInstance(String keyword){
        Integer code=MAP.get(keyword);
        if (code==null) return null;
        return new RtfCommand(code);
    }
    
    private final int code; 
    
    private RtfCommand(int code){
        this.code=code;
    }
    
    /**
     * furnish the command type
     * @return command type
     */
    int getCommandType(){
        if (code<FIRST_TYPE) return INSERTION_CHAR;
        return code&0x1FFF0;//-FIRST_TYPE;
    }
    
    /**
     * furnish the character to insert for an insertion command
     * @return character to insert
     */
    char getInsertionChar(){
        if ((code>=0)&&(code<FIRST_TYPE)) return (char)code;
        return '\uFFFF';
    }
    
    /**
     * For a CHARSET type command, furnishes corresponding Charset
     * @return the Charset
     */
    Charset getCharset(){
        return getCharset(getCharsetName());
    }
    
    /**
     * For a CHARSET_FROM type command, furnishes windows numbered Charset
     * @param number to insert in charset name after windows-
     * @return 
     */
    static Charset getCharsetFrom(String number){
        return getCharset("Cp"+number);
    }
    
    private static final int CHARSET_ANSI=1;
    private static final int CHARSET_MAC=2;
    private static final int CHARSET_PC=3;
    private static final int CHARSET_PCA=4;
    
    /**
     * for CHARSET type command,furnishes Charset name
     * @return Charset name
     */
    String getCharsetName(){
        switch (code&0xF){
             case CHARSET_ANSI: // ansi
                return "iso-8859-1"; // ou Cp1852 ?
            case CHARSET_MAC : // mac
                return "MacRoman";
            case CHARSET_PC : // pc
                return "Cp437";
            case CHARSET_PCA : // pca
                return "Cp850";
            default:
                return null;
        }
    }
    
      
    /**
     * Furnished Charset for name, if implemented in Java, else null
     * @param name Charset name
     * @return the Charset
     */
    static Charset getCharset(String name){
        try {
            if (name!=null) return Charset.forName(name);
        }
        catch (UnsupportedCharsetException ex){
        }
        return null;
    }
    
    static boolean isUnicodeCommand(String command){
        return "u".equals(command);
    }
    
    
    private static final Map<String,Integer> MAP=new HashMap<>();
    
    static{
         // SUBSTITUTION
        MAP.put("emdash",(int) '\u2014');//emdash("emdash", CommandType.Symbol), case emdash:processCharacter('\u2014');
        MAP.put("emspace",(int) '\u2003');//emspace("emspace", CommandType.Symbol),case emspace:processCharacter('\u2003');
        MAP.put("endash", (int)'\u2013');//endash("endash", CommandType.Symbol),case endash:processCharacter('\u2013');
        MAP.put("enspace",(int) '\u2002');//enspace("enspace", CommandType.Symbol),case enspace: processCharacter('\u2002');
        MAP.put("\\",(int) '\\');//backslash("\\", CommandType.Symbol),case backslash:processCharacter('\\');
        MAP.put("{",(int) '{');//opencurly("{", CommandType.Symbol),case opencurly:processCharacter('{');
        MAP.put("}",(int) '}');//closecurly("}", CommandType.Symbol),case closecurly:processCharacter('}');
        MAP.put("qmspace",(int) '\u2005');//qmspace("qmspace", CommandType.Symbol),case qmspace:processCharacter('\u2005');
        MAP.put("lquote",(int) '\u2018');//lquote("lquote", CommandType.Symbol),case lquote:processCharacter('\u2018');
        MAP.put("ldblquote",(int) '\u201c');//ldblquote("ldblquote", CommandType.Symbol),case ldblquote:processCharacter('\u201c');
        MAP.put("rquote",(int) '\u2019');//rquote("rquote", CommandType.Symbol),case rquote:processCharacter('\u2019');
        MAP.put("rdblquote",(int) '\u201d');//rdblquote("rdblquote", CommandType.Symbol),case rdblquote:processCharacter('\u201d');
        MAP.put("bullet",(int) '\u2022');//bullet("bullet", CommandType.Symbol),case bullet:processCharacter('\u2022');
        MAP.put("par",(int) '\n');//par("par", CommandType.Symbol),case par:processCharacter('\n');
        MAP.put("line",(int) '\n');//line("line", CommandType.Symbol),    case line:processCharacter('\n');
        MAP.put("row",(int) '\n');//row("row", CommandType.Symbol),    case row: processCharacter('\n');
        MAP.put("tab",(int) '\t');//tab("tab", CommandType.Symbol),case tab:processCharacter('\t');
        MAP.put("cell",(int) '\t');//cell("cell", CommandType.Symbol),    case cell:processCharacter('\t');
        MAP.put("\r",(int)'\n');
        MAP.put("\n",(int)'\n');
        // UNICODE
        MAP.put("u" ,UNICODE_COMMAND);//u("u", CommandType.Value),
        // CHARSETS
        MAP.put("ansicpg", CHARSET_FROM);
        MAP.put("ansi", CHARSET+CHARSET_ANSI);
        MAP.put("mac", CHARSET+CHARSET_MAC);
        MAP.put("pc", CHARSET+CHARSET_PC);
        MAP.put("pca", CHARSET+CHARSET_PCA);
        // TEXT DESTINATION        
        MAP.put( "rtf",TEXT_DEST);//rtf("rtf", CommandType.Destination),
        MAP.put("fldrslt" ,TEXT_DEST);//fldrslt("fldrslt", CommandType.Destination),
        MAP.put("pntext" ,TEXT_DEST);//pntext("pntext", CommandType.Destination),
        // NO TEXT DESTINATION
        MAP.put( "aftncn",NO_TEXT_DEST);//aftncn("aftncn", CommandType.Destination),   
        MAP.put("aftnsep" ,NO_TEXT_DEST);//aftnsep("aftnsep", CommandType.Destination),
        MAP.put("aftnsepc" ,NO_TEXT_DEST);//aftnsepc("aftnsepc", CommandType.Destination),   
        MAP.put( "annotation",NO_TEXT_DEST);//annotation("annotation", CommandType.Destination),   
        MAP.put("atnauthor" ,NO_TEXT_DEST);//atnauthor("atnauthor", CommandType.Destination),
        MAP.put("atndate" ,NO_TEXT_DEST);//atndate("atndate", CommandType.Destination),
        MAP.put("atnicn" ,NO_TEXT_DEST);//atnicn("atnicn", CommandType.Destination),
        MAP.put("atnid" ,NO_TEXT_DEST);//atnid("atnid", CommandType.Destination),
        MAP.put("atnparent" ,NO_TEXT_DEST);//atnparent("atnparent", CommandType.Destination),
        MAP.put( "atnref",NO_TEXT_DEST);//atnref("atnref", CommandType.Destination),
        MAP.put("atntime" ,NO_TEXT_DEST);//atntime("atntime", CommandType.Destination),
        MAP.put("atrfend" ,NO_TEXT_DEST);//atrfend("atrfend", CommandType.Destination),
        MAP.put( "atrfstart",NO_TEXT_DEST);//atrfstart("atrfstart", CommandType.Destination),
        MAP.put("author" ,NO_TEXT_DEST);//author("author", CommandType.Destination),
        MAP.put("background" ,NO_TEXT_DEST);//background("background", CommandType.Destination),
        MAP.put("bkmkend" ,NO_TEXT_DEST);//bkmkend("bkmkend", CommandType.Destination),
        MAP.put("bkmkstart" ,NO_TEXT_DEST);//bkmkstart("bkmkstart", CommandType.Destination),   
        MAP.put("blipuid" ,NO_TEXT_DEST);//blipuid("blipuid", CommandType.Destination),
        MAP.put("buptim" ,NO_TEXT_DEST);//buptim("buptim", CommandType.Destination),
        MAP.put("category" ,NO_TEXT_DEST);//category("category", CommandType.Destination),
        MAP.put("colorschememapping" ,NO_TEXT_DEST);//colorschememapping("colorschememapping", CommandType.Destination),
        MAP.put( "colortbl",NO_TEXT_DEST);//colortbl("colortbl", CommandType.Destination),
        MAP.put( "comment",NO_TEXT_DEST);//comment("comment", CommandType.Destination),
        MAP.put("company" ,NO_TEXT_DEST);//company("company", CommandType.Destination),
        MAP.put("creatim" ,NO_TEXT_DEST);//creatim("creatim", CommandType.Destination),
        MAP.put( "datafield",NO_TEXT_DEST);//datafield("datafield", CommandType.Destination),
        MAP.put( "datastore",NO_TEXT_DEST);//datastore("datastore", CommandType.Destination),
        MAP.put("defchp" ,NO_TEXT_DEST);//defchp("defchp", CommandType.Destination),
        MAP.put("defpap" ,NO_TEXT_DEST);//defpap("defpap", CommandType.Destination),
        MAP.put( "do",NO_TEXT_DEST);//docmd("do", CommandType.Destination),
        MAP.put("doccomm" ,NO_TEXT_DEST);//doccomm("doccomm", CommandType.Destination),
        MAP.put("docvar" ,NO_TEXT_DEST);//docvar("docvar", CommandType.Destination),
        MAP.put( "dptxbxtext",NO_TEXT_DEST);//dptxbxtext("dptxbxtext", CommandType.Destination),
        MAP.put( "ebcend",NO_TEXT_DEST);//ebcend("ebcend", CommandType.Destination),
        MAP.put("ebcstart" ,NO_TEXT_DEST);//ebcstart("ebcstart", CommandType.Destination),
        MAP.put("factoidname" ,NO_TEXT_DEST);//factoidname("factoidname", CommandType.Destination),
        MAP.put("falt" ,NO_TEXT_DEST);//falt("falt", CommandType.Destination),
        MAP.put("fchars" ,NO_TEXT_DEST);//fchars("fchars", CommandType.Destination),
        MAP.put( "ffdeftext",NO_TEXT_DEST);//ffdeftext("ffdeftext", CommandType.Destination),
        MAP.put( "ffentrymcr",NO_TEXT_DEST);//ffentrymcr("ffentrymcr", CommandType.Destination),
        MAP.put("ffexitmcr" ,NO_TEXT_DEST);//ffexitmcr("ffexitmcr", CommandType.Destination),
        MAP.put("ffformat" ,NO_TEXT_DEST);//ffformat("ffformat", CommandType.Destination),
        MAP.put( "ffhelptext",NO_TEXT_DEST);//ffhelptext("ffhelptext", CommandType.Destination),
        MAP.put("ffl" ,NO_TEXT_DEST);//ffl("ffl", CommandType.Destination),   
        MAP.put( "ffname",NO_TEXT_DEST);//ffname("ffname", CommandType.Destination),
        MAP.put("ffstattext" ,NO_TEXT_DEST);//ffstattext("ffstattext", CommandType.Destination),
        MAP.put("field" ,NO_TEXT_DEST);//field("field", CommandType.Destination),
        MAP.put("file" ,NO_TEXT_DEST);//file("file", CommandType.Destination),
        MAP.put( "filetbl",NO_TEXT_DEST);//filetbl("filetbl", CommandType.Destination),
        MAP.put("fldinst" ,NO_TEXT_DEST);//fldinst("fldinst", CommandType.Destination),        
        MAP.put("fldtype" ,NO_TEXT_DEST);//fldtype("fldtype", CommandType.Destination),
        MAP.put("fname" ,NO_TEXT_DEST);//fname("fname", CommandType.Destination),
        MAP.put("fontemb" ,NO_TEXT_DEST);//fontemb("fontemb", CommandType.Destination),   
        MAP.put( "fontfile",NO_TEXT_DEST);//fontfile("fontfile", CommandType.Destination),
        MAP.put( "fonttbl",NO_TEXT_DEST);//fonttbl("fonttbl", CommandType.Destination),
        MAP.put("footer" ,NO_TEXT_DEST);//footer("footer", CommandType.Destination),
        MAP.put( "footerf",NO_TEXT_DEST);//footerf("footerf", CommandType.Destination),
        MAP.put("footerl" ,NO_TEXT_DEST);//footerl("footerl", CommandType.Destination),
        MAP.put("footerr" ,NO_TEXT_DEST);//footerr("footerr", CommandType.Destination),
        MAP.put("formfield" ,NO_TEXT_DEST);//formfield("formfield", CommandType.Destination),
        MAP.put("footnote" ,NO_TEXT_DEST);//footnote("footnote", CommandType.Destination),
        MAP.put( "ftncn",NO_TEXT_DEST);//ftncn("ftncn", CommandType.Destination),
        MAP.put( "ftnsep",NO_TEXT_DEST);//ftnsep("ftnsep", CommandType.Destination),
        MAP.put("ftnsepc" ,NO_TEXT_DEST);//ftnsepc("ftnsepc", CommandType.Destination),   
        MAP.put("g" ,NO_TEXT_DEST);//g("g", CommandType.Destination),
        MAP.put("generator" ,NO_TEXT_DEST);//generator("generator", CommandType.Destination),
        MAP.put("gridtbl" ,NO_TEXT_DEST);//gridtbl("gridtbl", CommandType.Destination),
        MAP.put("header" ,NO_TEXT_DEST);//header("header", CommandType.Destination),
        MAP.put( "headerf",NO_TEXT_DEST);//headerf("headerf", CommandType.Destination),
        MAP.put( "headerl",NO_TEXT_DEST);//headerl("headerl", CommandType.Destination),
        MAP.put("headerr" ,NO_TEXT_DEST);//headerr("headerr", CommandType.Destination),
        MAP.put("hl" ,NO_TEXT_DEST);//hl("hl", CommandType.Destination),
        MAP.put("hlfr" ,NO_TEXT_DEST);//hlfr("hlfr", CommandType.Destination),
        MAP.put("hlinkbase" ,NO_TEXT_DEST);//hlinkbase("hlinkbase", CommandType.Destination),
        MAP.put("hlloc" ,NO_TEXT_DEST);//hlloc("hlloc", CommandType.Destination),
        MAP.put("hlsrc" ,NO_TEXT_DEST);//hlsrc("hlsrc", CommandType.Destination),
        MAP.put("hsv" ,NO_TEXT_DEST);//hsv("hsv", CommandType.Destination),
        MAP.put("htmltag" ,NO_TEXT_DEST);//htmltag("htmltag", CommandType.Destination),
        MAP.put("info" ,NO_TEXT_DEST);//info("info", CommandType.Destination),
        MAP.put( "keycode",NO_TEXT_DEST);//keycode("keycode", CommandType.Destination),
        MAP.put("keywords" ,NO_TEXT_DEST);//keywords("keywords", CommandType.Destination),
        MAP.put( "latentstyles",NO_TEXT_DEST);//latentstyles("latentstyles", CommandType.Destination),
        MAP.put("lchars" ,NO_TEXT_DEST);//lchars("lchars", CommandType.Destination), 
        MAP.put("levelnumbers" ,NO_TEXT_DEST);//levelnumbers("levelnumbers", CommandType.Destination),
        MAP.put("leveltext" ,NO_TEXT_DEST);//leveltext("leveltext", CommandType.Destination),
        MAP.put("lfolevel" ,NO_TEXT_DEST);//lfolevel("lfolevel", CommandType.Destination),
        MAP.put("linkval" ,NO_TEXT_DEST);//linkval("linkval", CommandType.Destination),
        MAP.put( "list",NO_TEXT_DEST);//list("list", CommandType.Destination),
        MAP.put("listlevel" ,NO_TEXT_DEST);//listlevel("listlevel", CommandType.Destination),
        MAP.put("listname" ,NO_TEXT_DEST);//listname("listname", CommandType.Destination),
        MAP.put( "listoverride",NO_TEXT_DEST);//listoverride("listoverride", CommandType.Destination),
        MAP.put( "listoverridetable",NO_TEXT_DEST);//listoverridetable("listoverridetable", CommandType.Destination),
        MAP.put( "listpicture",NO_TEXT_DEST);//listpicture("listpicture", CommandType.Destination),
        MAP.put( "liststylename",NO_TEXT_DEST);//liststylename("liststylename", CommandType.Destination),
        MAP.put("listtable" ,NO_TEXT_DEST);//listtable("listtable", CommandType.Destination),
        MAP.put( "listtext",NO_TEXT_DEST);//listtext("listtext", CommandType.Destination),
        MAP.put("lsdlockedexcept" ,NO_TEXT_DEST);//lsdlockedexcept("lsdlockedexcept", CommandType.Destination),
        MAP.put( "macc",NO_TEXT_DEST);//macc("macc", CommandType.Destination),
        MAP.put("maccPr" ,NO_TEXT_DEST);//maccPr("maccPr", CommandType.Destination),
        MAP.put( "mailmerge",NO_TEXT_DEST);//mailmerge("mailmerge", CommandType.Destination),
        MAP.put("maln" ,NO_TEXT_DEST);//maln("maln", CommandType.Destination),
        MAP.put( "malnScr",NO_TEXT_DEST);//malnScr("malnScr", CommandType.Destination),
        MAP.put("manager" ,NO_TEXT_DEST);//manager("manager", CommandType.Destination),   
        MAP.put("margPr" ,NO_TEXT_DEST);//margPr("margPr", CommandType.Destination),
        MAP.put( "mbar",NO_TEXT_DEST);//mbar("mbar", CommandType.Destination),
        MAP.put("mbarPr" ,NO_TEXT_DEST);//mbarPr("mbarPr", CommandType.Destination),
        MAP.put("mbaseJc" ,NO_TEXT_DEST);//mbaseJc("mbaseJc", CommandType.Destination),
        MAP.put("mbegChr" ,NO_TEXT_DEST);//mbegChr("mbegChr", CommandType.Destination),
        MAP.put("mborderBox" ,NO_TEXT_DEST);//mborderBox("mborderBox", CommandType.Destination),
        MAP.put( "mborderBoxPr",NO_TEXT_DEST);//mborderBoxPr("mborderBoxPr", CommandType.Destination),
        MAP.put("mbox" ,NO_TEXT_DEST);//mbox("mbox", CommandType.Destination),
        MAP.put("mboxPr" ,NO_TEXT_DEST);//mboxPr("mboxPr", CommandType.Destination),
        MAP.put("mchr" ,NO_TEXT_DEST);//mchr("mchr", CommandType.Destination),
        MAP.put("mcount" ,NO_TEXT_DEST);//mcount("mcount", CommandType.Destination),
        MAP.put("mctrlPr" ,NO_TEXT_DEST);//mctrlPr("mctrlPr", CommandType.Destination),
        MAP.put("md" ,NO_TEXT_DEST);//md("md", CommandType.Destination),
        MAP.put( "mdeg",NO_TEXT_DEST);//mdeg("mdeg", CommandType.Destination),
        MAP.put( "mdegHide",NO_TEXT_DEST);//mdegHide("mdegHide", CommandType.Destination),
        MAP.put("mden" ,NO_TEXT_DEST);//mden("mden", CommandType.Destination),
        MAP.put("mdiff" ,NO_TEXT_DEST);//mdiff("mdiff", CommandType.Destination),
        MAP.put("mdPr" ,NO_TEXT_DEST);//mdPr("mdPr", CommandType.Destination),
        MAP.put( "me",NO_TEXT_DEST);//me("me", CommandType.Destination),
        MAP.put("mendChr" ,NO_TEXT_DEST);//mendChr("mendChr", CommandType.Destination),
        MAP.put("meqArr" ,NO_TEXT_DEST);//meqArr("meqArr", CommandType.Destination),
        MAP.put( "meqArrPr",NO_TEXT_DEST);//meqArrPr("meqArrPr", CommandType.Destination),
        MAP.put("mf" ,NO_TEXT_DEST);//mf("mf", CommandType.Destination),
        MAP.put( "mfName",NO_TEXT_DEST);//mfName("mfName", CommandType.Destination),
        MAP.put("mfPr" ,NO_TEXT_DEST);//mfPr("mfPr", CommandType.Destination),
        MAP.put("mfunc" ,NO_TEXT_DEST);//mfunc("mfunc", CommandType.Destination),
        MAP.put("mfuncPr" ,NO_TEXT_DEST);//mfuncPr("mfuncPr", CommandType.Destination),
        MAP.put("mgroupChr" ,NO_TEXT_DEST);//mgroupChr("mgroupChr", CommandType.Destination),
        MAP.put("mgroupChrPr" ,NO_TEXT_DEST);//mgroupChrPr("mgroupChrPr", CommandType.Destination),
        MAP.put("mgrow" ,NO_TEXT_DEST);//mgrow("mgrow", CommandType.Destination),
        MAP.put( "mhideBot",NO_TEXT_DEST);//mhideBot("mhideBot", CommandType.Destination),
        MAP.put("mhideLeft" ,NO_TEXT_DEST);//mhideLeft("mhideLeft", CommandType.Destination),
        MAP.put("mhideRight" ,NO_TEXT_DEST);//mhideRight("mhideRight", CommandType.Destination),
        MAP.put("mhideTop" ,NO_TEXT_DEST);//mhideTop("mhideTop", CommandType.Destination),
        MAP.put( "mhtmltag",NO_TEXT_DEST);//mhtmltag("mhtmltag", CommandType.Destination),
        MAP.put( "mlim",NO_TEXT_DEST);//mlim("mlim", CommandType.Destination),
        MAP.put("mlimloc" ,NO_TEXT_DEST);//mlimloc("mlimloc", CommandType.Destination),
        MAP.put("mlimlow" ,NO_TEXT_DEST);//mlimlow("mlimlow", CommandType.Destination),
        MAP.put( "mlimlowPr",NO_TEXT_DEST);//mlimlowPr("mlimlowPr", CommandType.Destination),
        MAP.put("mlimupp" ,NO_TEXT_DEST);//mlimupp("mlimupp", CommandType.Destination),
        MAP.put( "mlimuppPr",NO_TEXT_DEST);//mlimuppPr("mlimuppPr", CommandType.Destination),
        MAP.put("mm" ,NO_TEXT_DEST);//mm("mm", CommandType.Destination),
        MAP.put("mmaddfieldname" ,NO_TEXT_DEST);//mmaddfieldname("mmaddfieldname", CommandType.Destination),
        MAP.put("mmath" ,NO_TEXT_DEST);//mmath("mmath", CommandType.Destination),
        MAP.put("mmathPict" ,NO_TEXT_DEST);//mmathPict("mmathPict", CommandType.Destination),
        MAP.put( "mmathPr",NO_TEXT_DEST);//mmathPr("mmathPr", CommandType.Destination),
        MAP.put("mmaxdist" ,NO_TEXT_DEST);//mmaxdist("mmaxdist", CommandType.Destination),
        MAP.put( "mmc",NO_TEXT_DEST);//mmc("mmc", CommandType.Destination),
        MAP.put("mmcJc" ,NO_TEXT_DEST);//mmcJc("mmcJc", CommandType.Destination),
        MAP.put("mmconnectstr" ,NO_TEXT_DEST);//mmconnectstr("mmconnectstr", CommandType.Destination),
        MAP.put( "mmconnectstrdata",NO_TEXT_DEST);//mmconnectstrdata("mmconnectstrdata", CommandType.Destination),
        MAP.put("mmcPr" ,NO_TEXT_DEST);//mmcPr("mmcPr", CommandType.Destination),
        MAP.put("mmcs" ,NO_TEXT_DEST);//mmcs("mmcs", CommandType.Destination),
        MAP.put("mmdatasource" ,NO_TEXT_DEST);//mmdatasource("mmdatasource", CommandType.Destination),
        MAP.put("mmheadersource" ,NO_TEXT_DEST);//mmheadersource("mmheadersource", CommandType.Destination),
        MAP.put("mmmailsubject" ,NO_TEXT_DEST);//mmmailsubject("mmmailsubject", CommandType.Destination),
        MAP.put( "mmodso",NO_TEXT_DEST);//mmodso("mmodso", CommandType.Destination),
        MAP.put("mmodsofilter" ,NO_TEXT_DEST);//mmodsofilter("mmodsofilter", CommandType.Destination),
        MAP.put("mmodsofldmpdata" ,NO_TEXT_DEST);//mmodsofldmpdata("mmodsofldmpdata", CommandType.Destination),
        MAP.put( "mmodsomappedname",NO_TEXT_DEST);//mmodsomappedname("mmodsomappedname", CommandType.Destination),
        MAP.put("mmodsoname" ,NO_TEXT_DEST);//mmodsoname("mmodsoname", CommandType.Destination),
        MAP.put("mmodsorecipdata" ,NO_TEXT_DEST);//mmodsorecipdata("mmodsorecipdata", CommandType.Destination),
        MAP.put("mmodsosort" ,NO_TEXT_DEST);//mmodsosort("mmodsosort", CommandType.Destination),
        MAP.put("mmodsosrc" ,NO_TEXT_DEST);//mmodsosrc("mmodsosrc", CommandType.Destination),
        MAP.put("mmodsotable" ,NO_TEXT_DEST);//mmodsotable("mmodsotable", CommandType.Destination),
        MAP.put( "mmodsotable",NO_TEXT_DEST);//mmodsoudl("mmodsotable", CommandType.Destination),
        MAP.put("mmodsoudldata" ,NO_TEXT_DEST);//mmodsoudldata("mmodsoudldata", CommandType.Destination),
        MAP.put("mmodsouniquetag" ,NO_TEXT_DEST);//mmodsouniquetag("mmodsouniquetag", CommandType.Destination),
        MAP.put( "mmPr",NO_TEXT_DEST);//mmPr("mmPr", CommandType.Destination),
        MAP.put("mmquery" ,NO_TEXT_DEST);// mmquery("mmquery", CommandType.Destination),
        MAP.put("mmr" ,NO_TEXT_DEST);//mmr("mmr", CommandType.Destination),
        MAP.put("mnary" ,NO_TEXT_DEST);//mnary("mnary", CommandType.Destination),
        MAP.put("mnaryPr" ,NO_TEXT_DEST);//mnaryPr("mnaryPr", CommandType.Destination),
        MAP.put("mnoBreak" ,NO_TEXT_DEST);//mnoBreak("mnoBreak", CommandType.Destination),
        MAP.put("mnum" ,NO_TEXT_DEST);//mnum("mnum", CommandType.Destination),
        MAP.put("mobjDist" ,NO_TEXT_DEST);//mobjDist("mobjDist", CommandType.Destination),
        MAP.put( "moMath",NO_TEXT_DEST);//moMath("moMath", CommandType.Destination),
        MAP.put("moMathPara" ,NO_TEXT_DEST);//moMathPara("moMathPara", CommandType.Destination),
        MAP.put("moMathParaPr" ,NO_TEXT_DEST);//moMathParaPr("moMathParaPr", CommandType.Destination),
        MAP.put("mopEmu" ,NO_TEXT_DEST);//mopEmu("mopEmu", CommandType.Destination),
        MAP.put("mphant" ,NO_TEXT_DEST);//mphant("mphant", CommandType.Destination),
        MAP.put( "mphantPr",NO_TEXT_DEST);//mphantPr("mphantPr", CommandType.Destination),
        MAP.put( "mplcHide",NO_TEXT_DEST);//mplcHide("mplcHide", CommandType.Destination),
        MAP.put("mpos" ,NO_TEXT_DEST);//mpos("mpos", CommandType.Destination),
        MAP.put( "mr",NO_TEXT_DEST);//mr("mr", CommandType.Destination),
        MAP.put( "mrad",NO_TEXT_DEST);//mrad("mrad", CommandType.Destination),
        MAP.put( "mradPr",NO_TEXT_DEST);//mradPr("mradPr", CommandType.Destination),
        MAP.put("mrPr" ,NO_TEXT_DEST);//mrPr("mrPr", CommandType.Destination),
        MAP.put("msepChr" ,NO_TEXT_DEST);//msepChr("msepChr", CommandType.Destination),
        MAP.put("mshow" ,NO_TEXT_DEST);//mshow("mshow", CommandType.Destination),
        MAP.put("mshp" ,NO_TEXT_DEST);//mshp("mshp", CommandType.Destination),
        MAP.put("msPre" ,NO_TEXT_DEST);//msPre("msPre", CommandType.Destination),
        MAP.put("msPrePr" ,NO_TEXT_DEST);//msPrePr("msPrePr", CommandType.Destination),
        MAP.put("msSub" ,NO_TEXT_DEST);//msSub("msSub", CommandType.Destination),
        MAP.put("msSubPr" ,NO_TEXT_DEST);//msSubPr("msSubPr", CommandType.Destination),
        MAP.put("msSubSup" ,NO_TEXT_DEST);//msSubSup("msSubSup", CommandType.Destination),
        MAP.put("msSubSupPr" ,NO_TEXT_DEST);//msSubSupPr("msSubSupPr", CommandType.Destination),
        MAP.put("msSup" ,NO_TEXT_DEST);//msSup("msSup", CommandType.Destination),
        MAP.put( "msSupPr",NO_TEXT_DEST);//msSupPr("msSupPr", CommandType.Destination),
        MAP.put("mstrikeBLTR" ,NO_TEXT_DEST);//mstrikeBLTR("mstrikeBLTR", CommandType.Destination),
        MAP.put("mstrikeH" ,NO_TEXT_DEST);//mstrikeH("mstrikeH", CommandType.Destination),
        MAP.put( "mstrikeTLBR",NO_TEXT_DEST);//mstrikeTLBR("mstrikeTLBR", CommandType.Destination),
        MAP.put("mstrikeV" ,NO_TEXT_DEST);//mstrikeV("mstrikeV", CommandType.Destination),
        MAP.put("msub" ,NO_TEXT_DEST);//msub("msub", CommandType.Destination),
        MAP.put("msubHide" ,NO_TEXT_DEST);//msubHide("msubHide", CommandType.Destination),
        MAP.put("msup" ,NO_TEXT_DEST);//msup("msup", CommandType.Destination),
        MAP.put( "msupHide",NO_TEXT_DEST);//msupHide("msupHide", CommandType.Destination),
        MAP.put( "mtransp",NO_TEXT_DEST);//mtransp("mtransp", CommandType.Destination),
        MAP.put("mtype" ,NO_TEXT_DEST);//mtype("mtype", CommandType.Destination),
        MAP.put("mvertJc" ,NO_TEXT_DEST);//mvertJc("mvertJc", CommandType.Destination),
        MAP.put("mvfmf" ,NO_TEXT_DEST);//mvfmf("mvfmf", CommandType.Destination),
        MAP.put("mvfml" ,NO_TEXT_DEST);//mvfml("mvfml", CommandType.Destination),
        MAP.put("mvtof" ,NO_TEXT_DEST);//mvtof("mvtof", CommandType.Destination),
        MAP.put("mvtol" ,NO_TEXT_DEST);//mvtol("mvtol", CommandType.Destination),
        MAP.put("mzeroAsc" ,NO_TEXT_DEST);//mzeroAsc("mzeroAsc", CommandType.Destination),
        MAP.put( "mzeroDesc",NO_TEXT_DEST);//mzeroDesc("mzeroDesc", CommandType.Destination),
        MAP.put("mzeroWid" ,NO_TEXT_DEST);//mzeroWid("mzeroWid", CommandType.Destination),
        MAP.put("nesttableprops" ,NO_TEXT_DEST);//nesttableprops("nesttableprops", CommandType.Destination),
        MAP.put("nextfile" ,NO_TEXT_DEST);//nextfile("nextfile", CommandType.Destination),
        MAP.put("nonesttables" ,NO_TEXT_DEST);//nonesttables("nonesttables", CommandType.Destination),
        MAP.put("objalias" ,NO_TEXT_DEST);//objalias("objalias", CommandType.Destination),
        MAP.put("objclass" ,NO_TEXT_DEST);//objclass("objclass", CommandType.Destination),
        MAP.put("objdata" ,NO_TEXT_DEST);//objdata("objdata", CommandType.Destination),
        MAP.put("object" ,NO_TEXT_DEST);//object("object", CommandType.Destination),
        MAP.put( "objname",NO_TEXT_DEST);//objname("objname", CommandType.Destination),
        MAP.put("objsect" ,NO_TEXT_DEST);//objsect("objsect", CommandType.Destination),
        MAP.put("objtime" ,NO_TEXT_DEST);//objtime("objtime", CommandType.Destination),
        MAP.put("oldcprops" ,NO_TEXT_DEST);//oldcprops("oldcprops", CommandType.Destination),
        MAP.put("oldpprops" ,NO_TEXT_DEST);//oldpprops("oldpprops", CommandType.Destination),
        MAP.put("oldsprops" ,NO_TEXT_DEST);//oldsprops("oldsprops", CommandType.Destination),
        MAP.put("oldtprops" ,NO_TEXT_DEST);//oldtprops("oldtprops", CommandType.Destination),
        MAP.put("oleclsid" ,NO_TEXT_DEST);//oleclsid("oleclsid", CommandType.Destination),
        MAP.put( "operator",NO_TEXT_DEST);//operator("operator", CommandType.Destination),
        MAP.put("panose" ,NO_TEXT_DEST);//panose("panose", CommandType.Destination),
        MAP.put("password" ,NO_TEXT_DEST);//password("password", CommandType.Destination),
        MAP.put("passwordhash" ,NO_TEXT_DEST);//passwordhash("passwordhash", CommandType.Destination),
        MAP.put("pgptbl" ,NO_TEXT_DEST);//pgptbl("pgptbl", CommandType.Destination),
        MAP.put("pgdsctbl" ,NO_TEXT_DEST);//pgdsctbl("pgdsctbl",CommandType.Destination), // ajout ppour Open office
        MAP.put( "picprop",NO_TEXT_DEST);//picprop("picprop", CommandType.Destination),
        MAP.put("pict" ,NO_TEXT_DEST);//pict("pict", CommandType.Destination),
        MAP.put("pn" ,NO_TEXT_DEST);//pn("pn", CommandType.Destination),
        MAP.put("pnseclvl" ,NO_TEXT_DEST);//pnseclvl("pnseclvl", CommandType.Destination), // and Value        
        MAP.put("pntxta" ,NO_TEXT_DEST);//pntxta("pntxta", CommandType.Destination),
        MAP.put( "pntxtb",NO_TEXT_DEST);//pntxtb("pntxtb", CommandType.Destination),
        MAP.put("printim" ,NO_TEXT_DEST);//printim("printim", CommandType.Destination),
        MAP.put( "private",NO_TEXT_DEST);//privatecmd("private", CommandType.Destination),
        MAP.put("propname" ,NO_TEXT_DEST);//propname("propname", CommandType.Destination),
        MAP.put("protstart" ,NO_TEXT_DEST);//   protstart("protstart", CommandType.Destination),
        MAP.put("protusertbl" ,NO_TEXT_DEST);//protusertbl("protusertbl", CommandType.Destination),
        MAP.put("pxe" ,NO_TEXT_DEST);//pxe("pxe", CommandType.Destination),
        MAP.put("result" ,NO_TEXT_DEST);//result("result", CommandType.Destination),
        MAP.put("revtbl" ,NO_TEXT_DEST);//   revtbl("revtbl", CommandType.Destination),
        MAP.put("revtim" ,NO_TEXT_DEST);//revtim("revtim", CommandType.Destination),
        MAP.put("rsidtbl" ,NO_TEXT_DEST);//rsidtbl("rsidtbl", CommandType.Destination),
        MAP.put( "rxe",NO_TEXT_DEST);//rxe("rxe", CommandType.Destination),
        MAP.put( "shp",NO_TEXT_DEST);//shp("shp", CommandType.Destination),
        MAP.put( "shpgrp",NO_TEXT_DEST);//shpgrp("shpgrp", CommandType.Destination),
        MAP.put( "shpinst",NO_TEXT_DEST);//shpinst("shpinst", CommandType.Destination),
        MAP.put("shppict" ,NO_TEXT_DEST);//shppict("shppict", CommandType.Destination),
        MAP.put("shprslt" ,NO_TEXT_DEST);//shprslt("shprslt", CommandType.Destination),
        MAP.put("shptxt" ,NO_TEXT_DEST);//shptxt("shptxt", CommandType.Destination),
        MAP.put("sn" ,NO_TEXT_DEST);//sn("sn", CommandType.Destination),
        MAP.put("sp" ,NO_TEXT_DEST);//sp("sp", CommandType.Destination),
        MAP.put( "staticval",NO_TEXT_DEST);//staticval("staticval", CommandType.Destination),
        MAP.put( "stylesheet",NO_TEXT_DEST);//stylesheet("stylesheet", CommandType.Destination),
        MAP.put("subject" ,NO_TEXT_DEST);//subject("subject", CommandType.Destination),
        MAP.put("sv" ,NO_TEXT_DEST);//sv("sv", CommandType.Destination),
        MAP.put("svb" ,NO_TEXT_DEST);//svb("svb", CommandType.Destination),
        MAP.put( "tc",NO_TEXT_DEST);//tc("tc", CommandType.Destination),
        MAP.put("template" ,NO_TEXT_DEST);//template("template", CommandType.Destination),
        MAP.put( "themedata",NO_TEXT_DEST);//themedata("themedata", CommandType.Destination),
        MAP.put("title" ,NO_TEXT_DEST);//title("title", CommandType.Destination),
        MAP.put("txe" ,NO_TEXT_DEST);//txe("txe", CommandType.Destination),       
        MAP.put("ud", NO_TEXT_DEST);//ud("ud", CommandType.Destination),
        MAP.put("upr" ,NO_TEXT_DEST);//upr("upr", CommandType.Destination),
        MAP.put("userprops" ,NO_TEXT_DEST);//userprops("userprops", CommandType.Destination),
        MAP.put("wgrffmtfilter" ,NO_TEXT_DEST);//wgrffmtfilter("wgrffmtfilter", CommandType.Destination),
        MAP.put("windowcaption" ,NO_TEXT_DEST);//windowcaption("windowcaption", CommandType.Destination),
        MAP.put( "writereservation",NO_TEXT_DEST);//writereservation("writereservation", CommandType.Destination),
        MAP.put("writereservhash" ,NO_TEXT_DEST);//writereservhash("writereservhash", CommandType.Destination),
        MAP.put("xe" ,NO_TEXT_DEST);//xe("xe", CommandType.Destination),
        MAP.put( "xform",NO_TEXT_DEST);//xform("xform", CommandType.Destination),
        MAP.put("xmlattrname",NO_TEXT_DEST);//xmlattrname("xmlattrname", CommandType.Destination),
        MAP.put("xmlattrvalue" ,NO_TEXT_DEST);//("xmlattrvalue", CommandType.Destination),
        MAP.put("xmlclose" ,NO_TEXT_DEST);//xmlclose("xmlclose", CommandType.Destination),
        MAP.put("xmlname" ,NO_TEXT_DEST);//xmlname("xmlname", CommandType.Destination),
        MAP.put("xmlnstbl" ,NO_TEXT_DEST);//xmlnstbl("xmlnstbl", CommandType.Destination),
        MAP.put("xmlopen" ,NO_TEXT_DEST);//xmlopen("xmlopen", CommandType.Destination) ;        
        
    }

}
