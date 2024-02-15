/**************************************************************************
 *  Copyright (C) 2024 by Richard Crook                                   *
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

/*************************************************************************************************/
/****************** Observable status with severity and associated text message ******************/
/*************************************************************************************************/

public class ObservableStatus implements ISignal
{
  private Level  m_severity; // severity of status
  private String m_msg;      // text message for status

  public static enum Level // status types
  {
    NORMAL, WARNING, ERROR, FATAL
  }

  private static final String STYLE_NORMAL  = "-fx-text-fill: black;";
  private static final String STYLE_WARNING = "-fx-text-fill: orange;";
  private static final String STYLE_ERROR   = "-fx-text-fill: red;";

  /**************************************** constructor ******************************************/
  public ObservableStatus()
  {
    // create empty status
    m_severity = Level.NORMAL;
    m_msg = null;
  }

  /**************************************** constructor ******************************************/
  public ObservableStatus( Level severity, String msg )
  {
    // create specified status
    m_severity = severity;
    m_msg = msg;
  }

  /******************************************* update ********************************************/
  public void update( Level severity, String msg )
  {
    // update severity and message
    if ( severity != m_severity || msg != m_msg )
    {
      m_severity = severity;
      m_msg = msg;
      signal();
    }
  }

  /******************************************** clear ********************************************/
  public void clear()
  {
    // clear status to normal severity and no message
    update( Level.NORMAL, null );
  }

  /******************************************* isError *******************************************/
  public Boolean isError()
  {
    // return if status in error state
    return m_severity == Level.ERROR || m_severity == Level.FATAL;
  }

  /***************************************** setSeverity *****************************************/
  public void setSeverity( Level severity )
  {
    // update severity
    update( severity, m_msg );
  }

  /***************************************** setMessage ******************************************/
  public void setMessage( String msg )
  {
    // update message
    update( m_severity, msg );
  }

  /***************************************** getSeverity *****************************************/
  public Level getSeverity()
  {
    // return status severity level
    return m_severity;
  }

  /***************************************** getMessage ******************************************/
  public String getMessage()
  {
    // return status text message
    return m_msg;
  }

  /****************************************** getStyle *******************************************/
  public String getStyle()
  {
    // return suitable style css for current severity
    switch ( m_severity )
    {
      case NORMAL:
        return STYLE_NORMAL;
      case WARNING:
        return STYLE_WARNING;
      case ERROR:
      case FATAL:
        return STYLE_ERROR;
      default:
        throw new UnsupportedOperationException( m_severity.toString() );
    }
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    // return as string
    return getClass().getSimpleName() + "@" + Integer.toHexString( System.identityHashCode( this ) ) + "[" + m_severity
        + " '" + m_msg + "']";
  }

}