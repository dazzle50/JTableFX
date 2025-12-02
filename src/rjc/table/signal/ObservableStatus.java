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

package rjc.table.signal;

import java.util.Timer;
import java.util.TimerTask;

import rjc.table.Utils;

/*************************************************************************************************/
/****************** Observable status with severity and associated text message ******************/
/*************************************************************************************************/

/**
 * Observable status with severity level and associated text message that can signal
 * listeners when changed. Provides automatic timer-based clearing functionality and
 * CSS styling support for different severity levels.
 */
public class ObservableStatus implements ISignal
{
  private Level  m_severity;            // severity of status
  private String m_msg;                 // text message for status
  private Timer  m_timer = new Timer(); // timer to clear temporary status

  // enumeration of status severity levels from info through fatal.
  public static enum Level // status types
  {
    INFO, WARNING, ERROR, FATAL
  }

  private static final String STYLE_INFO    = "-fx-text-fill: black;";
  private static final String STYLE_WARNING = "-fx-text-fill: maroon;";
  private static final String STYLE_ERROR   = "-fx-text-fill: red;";
  private static final String STYLE_FATAL   = "-fx-text-fill: white; -fx-font-weight: bold; -fx-background-color: red";

  /**************************************** constructor ******************************************/
  /**
   * Creates an observable status with normal severity and no message.
   */
  public ObservableStatus()
  {
    // create empty status
    m_severity = Level.INFO;
    m_msg = null;
  }

  /**************************************** constructor ******************************************/
  /**
   * Creates an observable status with the specified severity and message.
   * 
   * @param severity the initial severity level
   * @param msg the initial text message
   */
  public ObservableStatus( Level severity, String msg )
  {
    // create specified status
    m_severity = severity;
    m_msg = msg;
  }

  /******************************************* update ********************************************/
  /**
   * Updates the severity and message of this observable status.
   * If either value changes, cancels any pending timer and signals all listeners.
   * 
   * @param severity the new severity level
   * @param msg the new text message
   */
  public void update( Level severity, String msg )
  {
    // update severity and message
    if ( severity != m_severity || msg != m_msg )
    {
      m_severity = severity;
      m_msg = msg;
      m_timer.cancel();
      signal();
    }
  }

  /******************************************** clear ********************************************/
  /**
   * Clears the status to normal severity with no message.
   * Cancels any pending timer and signals listeners if the status changes.
   */
  public void clear()
  {
    // clear status to normal severity and no message
    update( Level.INFO, null );
  }

  /******************************************* isError *******************************************/
  /**
   * Checks if the current status represents an error condition.
   * 
   * @return true if severity is ERROR or FATAL, false otherwise
   */
  public Boolean isError()
  {
    // return if status in error state
    return m_severity == Level.ERROR || m_severity == Level.FATAL;
  }

  /***************************************** setSeverity *****************************************/
  /**
   * Sets a new severity level while keeping the current message.
   * 
   * @param severity the new severity level
   */
  public void setSeverity( Level severity )
  {
    // update severity
    update( severity, m_msg );
  }

  /***************************************** setMessage ******************************************/
  /**
   * Sets a new message while keeping the current severity level.
   * 
   * @param msg the new text message
   */
  public void setMessage( String msg )
  {
    // update message
    update( m_severity, msg );
  }

  /************************************* clearAfterMillisecs *************************************/
  /**
   * Schedules the status to be cleared after the specified number of milliseconds.
   * Cancels any previously scheduled clear operation. The clear will not occur
   * if the status is modified before the timer expires.
   * 
   * @param millisecs the delay in milliseconds before clearing the status
   */
  public void clearAfterMillisecs( int millisecs )
  {
    // clear the status after specified number of milliseconds (unless altered before)
    m_timer = new Timer();
    TimerTask task = new TimerTask()
    {
      @Override
      public void run()
      {
        clear();
      }
    };
    m_timer.schedule( task, millisecs );
  }

  /***************************************** getSeverity *****************************************/
  /**
   * Gets the current severity level of this observable status.
   * 
   * @return the current severity level
   */
  public Level getSeverity()
  {
    // return status severity level
    return m_severity;
  }

  /***************************************** getMessage ******************************************/
  /**
   * Gets the current text message of this observable status.
   * 
   * @return the current text message, may be null
   */
  public String getMessage()
  {
    // return status text message
    return m_msg;
  }

  /****************************************** getStyle *******************************************/
  /**
   * Gets the CSS style string appropriate for the specified severity level.
   * Provides consistent visual styling across different severity levels.
   * 
   * @param severity the severity level to get styling for
   * @return CSS style string for the specified severity
   * @throws UnsupportedOperationException if severity level is not recognised
   */
  public static String getStyle( Level severity )
  {
    // return suitable style css for specified severity
    switch ( severity )
    {
      case INFO:
        return STYLE_INFO;
      case WARNING:
        return STYLE_WARNING;
      case ERROR:
        return STYLE_ERROR;
      case FATAL:
        return STYLE_FATAL;
      default:
        throw new UnsupportedOperationException( severity.toString() );
    }
  }

  /****************************************** getStyle *******************************************/
  /**
   * Gets the CSS style string appropriate for the current severity level.
   * 
   * @return CSS style string for the current severity
   */
  public String getStyle()
  {
    // return suitable style css for current severity
    return getStyle( m_severity );
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    // return as string
    return Utils.name( this ) + "[" + m_severity + " '" + m_msg + "']";
  }

}