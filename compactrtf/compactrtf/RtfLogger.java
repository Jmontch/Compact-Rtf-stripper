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

import java.util.logging.Level;


/**
 *
 * @author Jmontch
 * 
 * This class is limited to furnish debug helps.
 * Calling functions log, info,warning or error simply write a message to Output
 * Other functions allow to specify a minimum level (levels of java class Level) to effectively write.
 * If you do not need debug help, you can suppress the calls to this class in RtfStripper
 * and delete this class;
 * When integrating compactRtf in application you can modify this class to do calls to application (or standard java) debug.
 */
public class RtfLogger {
    
    private static final int LEVEL_FINE=Level.FINE.intValue();
    private static final int LEVEL_INFO=Level.INFO.intValue();
    private static final int LEVEL_WARNING=Level.WARNING.intValue();
    private static final int LEVEL_SEVERE=Level.SEVERE.intValue();
    
    private static int LEVEL=LEVEL_WARNING;
    
    /**
     * specify a level which will attributed at objects creation
     * @param level the level (constzant of java class util.logging.Level)
     */
    public static void setGlobalLevel(Level level){
        LEVEL=level.intValue();
    }
    
    final private String className;
    private int curLevel;
    
    /**
     * constructor
     * @param className name which will be put at beginning of all messages
     */
    public RtfLogger(String className){
        this.className=className;
        curLevel=LEVEL;
    }
    
    /**
     * alternative constructor, the name is the furnished object class name
     * @param object furnished object expected not null
     */
    public RtfLogger(Object object){
        this(object.getClass().getName());
    }
    
    /**
     * set debug level for this object
     * @param level debug level from java class Level
     */
    public void setLevel(Level level){
        curLevel=level.intValue();
    }
    
    /**
     * write a FINE level message (il level >=FINE)
     * @param msg the message
     */
    public void log(String msg){
        trace(LEVEL_FINE,"Log",msg);
    }
    
    /**
     * write an INFO level message (if level>= info)
     * @param msg the message
     */
    public void info(String msg){
        trace(LEVEL_INFO,"Info",msg);
    }
    
    /**
     * write a WARNING level message (if level >=warning)
     * @param msg the message
     */
    public void warning(String msg){
        trace(LEVEL_WARNING,"Warning",msg);
    }
    
    /**
     * write an error message level (f level>=error)
     * @param msg the message
     */
    public void error(String msg){
        trace(LEVEL_SEVERE,"Error",msg);
    }
    
    /*
    write a message to System.out if message level greater or equal to object current level
    */
    private void trace(int level,String hdr,String msg){
        if (level>=curLevel)
            System.out.println(hdr+" class "+className+" "+msg);
    }
    
}
