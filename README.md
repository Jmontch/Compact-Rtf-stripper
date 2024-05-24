# Compact-Rtf-stripper
CompactRtfStripper is a compact java library to extract text parsing Rtf file content.
It extracts only the text, inserted objects and presentation directives are lost.

The objective is to have a Java package very easy to integrate in an application. Package contains only two classes, a third class is furnished for debug messages, it can be simply deleted or adapted to application debug system.

Only basic Java functionalities are used, the library was validated with JDK 1.8 and is used with Android Java.

The philosophy is to try to extract text as long as possible without raising exceptions when file is corrupted, extracted text may contain unexpected words, but you may have indicators in such a situation.

# 1 - Library content
Library contains 2 classes and an optional class :
- _RtfStripper_ class furnishes extracting functions,
- _RtfCommand_ class contains Rtf commands,
- optional _RtfLogger_ class is only for debug helps.

# 2 – Using library
To use this library, add to your application a package with the two classes _RtfStripper_ and _RtfCommand_.
If you have no need of debug helps, simply delete three lines at the end of _RtfStripper_ class.
Else add the _RtfLogger class_. The furnished code simply write warning messages to output. You can modify this class to change messages level, or call Java standard logger, or your application debug system.

To use library in your application, you have two options :
- for file of limited size, read the file in a Java String then call _RtfStripper_ static function _stripLimitedSource_, it returns a Java String with extracted text, and static function _getLastReturnCode_ allow to access to the return code,
- for larger files, create a _RtfStripper_ object, a java _Reader_ to read the file, and a java _Writer_ to write text extracted, and call _RtfStripper_ _stripSource_ function, it returns with a code when all text is extracted.

You can see some example of use in the main _Rtf_ class furnished with the library.

# 3 – About character sets
Rtf source uses only ASCII characters. When encountering not ASCII characters, Rtf generators replace them by hexadecimal command followed with 2 hexadecimal digits, or Unicode command followed by code in decimal. So, when as usual, file is coded with an ASCII extension 8 bits character set, there is no problem to convert it in Java String.
When parsing, characters from ASCII and Unicode command give Unicode characters without problems. But for hexadecimal coded, we have to translate given code to Unicode character. For this we must know the character set used by the Rtf generator.

Rtf generator inserts at source beginning some commands defining used character set. Commands "mac", "pc" and "pca" indicate a specific character set (respectively MacRoman, IBM Cp437, IBM Cp850), "ansicpg" followed by a number indicates a codepage number  character set such as Microsoft "Cp1252". But "ansi" is ambiguous, for some authors it is "ISO-8859-1", for others "Cp1252", this is not very important, there are very few differences between this two character sets and "ansi" is frequently followed by an "ansicpg" command. 

The parser interprets the characters set commands, retains the last encountered, and translates codes coming from hexadecimal in Unicode characters using Java library. but it may happen that generator character set is not supported by Java implementation, then code is converted in Java "char". 

But the major problem comes from characters not in generator 8 bits character set. Some generators put an Unicode command for each, but other have strange techniques of coding using hexadecimal… Result of parsing may be surprising, sometimes understandable …

# 4 – Other Rtf parsers
There is a Rtf parser in java standard library javax.swing.text.rtf, RTFEditorKit. But this kit is not documented, what it really does ? More it is not in all java implementations, for instance Android implementation, and it does not furnish good results.

I found on GitHub the project RtfParserKit (https://github.com/joniles/rtfparserkit). There is a Rtf parser, but with 6 packages and 34 classes or interfaces, it is complex, not easy to adapt in order to integrate in an application, and coding did not seem efficient. There are also some problems in results.

So I have based my project on their algorithms, and I have rewritten code in order to obtain a compact and efficient software. The result is very compact and adaptable library with only two class (and an optional class for debug). I have tried to take in account a maximum number of encountered coding techniques, but I cannot had good results with some strange coding techniques.

# 5 – Classes presentation
## 5.1 - Class RtfStripper
This class is a part of a compactRtf library to extract text from a Rtf file content. The library contains this class, the _RtfCommand_ class which contains Rtf commands, and optional _RtfLogger_ class which is only for debug helps.

This class parses  Rtf source, interprets commands,and write extracted text. The main function is _stripSource_, which has as parameters a Java _Reader_ to read source, and a Java _Writer_ to write extracted text character by character. It checks if the first characters are the first characters of a Rtf file "{\rtf", and if no, the return is code NO_RTF.

For file of limited size, already in memory or being read in a single bloc, static function _stripLimitedSource_ with source in String as parameter,  does _stripSource_ calling and returns extracted text. More, static function _getLastReturnCode_ furnishes the return code of the last call to _stripLimitedSource_. 

In addition some utility function are made public (Rtf start sequence check, last EOL delete).

Note also that if a problem is detected, probably corrupted data, no Exception are thrown, and class try to continue to extract text. Result may be incorrect, but the return code CORRUPTED_RTF signals that a problem has been detected.
## 5.2 - Class RtfCommand
These class allows interpreting commands found in Rtf source.

Commands are classed in six types :
- INSERTION command to insert a specified character in out text,
- UNICODE command to insert a character whose unicode code follows the command,
- TEXT_DESTINATION indicating that text after this command is to insert in out text,
- NO_TEXT_DESTINATION indicating that text which follows is not to insert,
- CHARSET defining a standard character set, with selection from 1 to 4,
- CHARSET_FROM define a character set with name "Cpxxxx", where xxxx is a number that follows the command.

The class construct a Map (command text, code) for all commands. Code is the character to insert, unicode code for UNICODE command, and a special code (>10000 hexa) indicating type for other.

For DESTINATION commands, all known commands are retained. For INSERTION and UNICODE commands, only them that have action on out text are retained.
## 5.3 - Class RtfLogger
This class is limited to furnish debug helps.

Calling functions _log_, _info_, _warning_ or _error_ simply write a message to output.

Other functions allow to specify a minimum level (levels of java class _Level_) to effectively write.

If you do not need debug help, you can suppress the calls to this class at the end of _RtfStripper_ class and delete this class.

When integrating CompactRtfStripper in application you can modify this class to do calls to application (or standard java) debug.
