/**************************************************************************
 *  Copyright (C) 2025 by Richard Crook                                   *
 *  https://github.com/dazzle50/JTableFX                                  *
 *                                                                        *
 *  This program is free software: you can redistribute it and/or modify  *
 *  it under the terms of the GNU General Public License as published by  *
 *  the Free Software Foundation, either version 3 of the License, or     *
 *  (at your option) any later version.                                   *
 *                                                                        *
 *  This program is distributed in the hope that it will be useful,       *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *  GNU General Public License for more details.                          *
 *                                                                        *
 *  You should have received a copy of the GNU General Public License     *
 *  along with this program.  If not, see http://www.gnu.org/licenses/    *
 **************************************************************************/

package rjc.table;

import java.lang.StackWalker.StackFrame;
import java.lang.reflect.Array;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/*************************************************************************************************/
/******************* Miscellaneous utility public static methods and variables *******************/
/*************************************************************************************************/

public class Utils
{
  public static final String             VERSION      = "v0.1.2";

  // pre-compiled formatter and pattern for better performance
  private static final DateTimeFormatter TIMESTAMP    = DateTimeFormatter.ofPattern( "uuuu-MM-dd HH:mm:ss.SSS " );
  private static final Pattern           WHITESPACE   = Pattern.compile( "(\\s+)" );

  // stack walking configuration
  private static final String            EARLY_EXIT   = "com.sun.javafx.application.LauncherImpl";
  private static final String            SKIP_ISIGNAL = "rjc.table.signal.ISignal$SignalHelper";
  private static final StackWalker       STACK_WALKER = StackWalker.getInstance();

  /****************************************** timestamp ******************************************/
  public static String timestamp()
  {
    // returns current date-time as string in format YYYY-MM-DD HH:MM:SS.SSS
    return LocalDateTime.now().format( TIMESTAMP );
  }

  /******************************************** trace ********************************************/
  public static void trace( Object... objects )
  {
    // sends to standard out date-time, the input objects, suffixed by file+line-number & method
    String caller = STACK_WALKER.walk( stream ->
    {
      var iterator = stream.iterator();
      iterator.next(); // skip trace()
      return stackFrameString( iterator.next() );
    } );
    System.out.println( timestamp() + objectsString( objects ) + caller );
  }

  /********************************************* path ********************************************/
  public static void path( Object... objects )
  {
    // sends to standard out date-time, the input objects, and simplified stack path
    String path = STACK_WALKER.walk( stream ->
    {
      StringBuilder str = new StringBuilder();
      var iterator = stream.iterator();
      iterator.next(); // skip path()

      while ( iterator.hasNext() )
      {
        StackFrame frame = iterator.next();
        String className = frame.getClassName();
        if ( EARLY_EXIT.equals( className ) )
          break;
        if ( !SKIP_ISIGNAL.equals( className ) )
          str.append( stackFrameString( frame ) );
      }
      return str.toString();
    } );
    System.out.println( timestamp() + objectsString( objects ) + path );
  }

  /******************************************* stack *********************************************/
  public static void stack( Object... objects )
  {
    // sends to standard out date-time and the input objects
    System.out.println( timestamp() + objectsString( objects ) );

    // sends to standard out this thread's stack trace
    STACK_WALKER.walk( stream ->
    {
      var iterator = stream.iterator();
      iterator.next(); // skip stack()
      while ( iterator.hasNext() )
        System.out.println( "\t" + iterator.next() );
      return null; // nothing to return
    } );
  }

  /**************************************** stackFrameString ****************************************/
  private static String stackFrameString( StackFrame frame )
  {
    // return frame as string in desired format
    return " (" + frame.getFileName() + ":" + frame.getLineNumber() + ") " + frame.getMethodName() + "()";
  }

  /**************************************** objectsString ****************************************/
  public static StringBuilder objectsString( Object... objects )
  {
    // converts objects to space separated string
    StringBuilder str = new StringBuilder();
    for ( Object obj : objects )
    {
      if ( obj == null )
        str.append( "null " );
      else if ( obj instanceof String s )
        str.append( '"' ).append( s ).append( "\" " );
      else if ( obj instanceof Character c )
        str.append( '\'' ).append( c ).append( "' " );
      else if ( obj.getClass().isArray() )
      {
        if ( obj.getClass().getComponentType().isPrimitive() )
        {
          // convert array of primitives into object list (max 20 items)
          int len = Array.getLength( obj );
          int box = clamp( len, 0, 20 );
          List<Object> list = new ArrayList<>( box );
          for ( int i = 0; i < box; i++ )
            list.add( Array.get( obj, i ) );
          if ( box < len )
            list.add( "...of " + len );
          str.append( objectsString( list ) ).append( " " );
        }
        else
          str.append( "[" ).append( objectsString( (Object[]) obj ) ).append( "] " );
      }
      else
        str.append( obj ).append( " " );
    }

    // remove excess space character at end if present
    if ( str.length() > 0 )
      str.deleteCharAt( str.length() - 1 );
    return str;
  }

  /******************************************* clean *********************************************/
  public static String clean( String txt )
  {
    // returns a clean string
    return WHITESPACE.matcher( txt.trim() ).replaceAll( " " );
  }

  /******************************************** name *********************************************/
  public static String name( Object obj )
  {
    // returns the objects simple class name with hash identity in hex
    return obj.getClass().getSimpleName() + "@" + Integer.toHexString( System.identityHashCode( obj ) );
  }

  /******************************************** clamp ********************************************/
  public static int clamp( int val, int min, int max )
  {
    // return integer clamped between supplied min and max
    return val > max ? max : val < min ? min : val;
  }

  public static double clamp( double val, double min, double max )
  {
    // return double clamped between supplied min and max
    return val > max ? max : val < min ? min : val;
  }

}