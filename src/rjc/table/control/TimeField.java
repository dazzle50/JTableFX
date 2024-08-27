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

package rjc.table.control;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import rjc.table.data.types.Time;
import rjc.table.signal.ISignal;
import rjc.table.signal.ObservableStatus;
import rjc.table.signal.ObservableStatus.Level;

public class TimeField extends ButtonField implements ISignal
{
  private Time m_time; // field current time (or most recent valid)

  /**************************************** constructor ******************************************/
  public TimeField( ObservableStatus status )
  {
    // construct field
    setStatus( status );
    setButtonType( ButtonType.DOWN );
    new TEMP_DateTimeDropDown( this );

    // react to text changes, for example user typing new time
    textProperty().addListener( ( property, oldText, newText ) ->
    {
      try
      {
        // if no exception raised and time is different send signal (but don't update text)
        Time time = Time.fromString( newText );
        if ( !time.equals( m_time ) )
        {
          m_time = time;
          signal( time );
        }

        getStatus().update( Level.NORMAL, "Time: " + formatStatus( time ) );
        setStyle( getStatus().getStyle() );
      }
      catch ( Exception exception )
      {
        getStatus().update( Level.ERROR, "Time is not valid" );
        setStyle( getStatus().getStyle() );
      }
    } );

    // react to focus change to ensure text shows time in correct format
    focusedProperty().addListener( ( property, oldF, newF ) ->
    {
      setText( format( m_time ) );
      positionCaret( getText().length() );
      getStatus().update( Level.NORMAL, null );
    } );

    // react to key presses
    addEventFilter( KeyEvent.KEY_PRESSED, event ->
    {
      if ( event.getCode() == KeyCode.UP )
      {
        event.consume();
        changeValue( 1, event.isShiftDown(), event.isControlDown(), event.isAltDown() );
      }

      if ( event.getCode() == KeyCode.DOWN )
      {
        event.consume();
        changeValue( -1, event.isShiftDown(), event.isControlDown(), event.isAltDown() );
      }

      if ( event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.ESCAPE )
      {
        setText( format( m_time ) );
        positionCaret( getText().length() );
      }
    } );

    // set initial time truncated to hour
    setTime( Time.fromHours( Time.now().getHours() ) );
  }

  /****************************************** getTime ********************************************/
  public Time getTime()
  {
    // return field time (or most recent valid)
    return m_time;
  }

  /****************************************** setTime ********************************************/
  public void setTime( Time time )
  {
    // set current field time, display in text, signal change
    if ( !time.equals( m_time ) )
    {
      m_time = time;
      setText( format( time ) );
      positionCaret( getText().length() );
      signal( time );
    }
  }

  /******************************************* format ********************************************/
  public String format( Time time )
  {
    // return time in display format
    return time.toString();
  }

  /**************************************** formatStatus *****************************************/
  public String formatStatus( Time time )
  {
    // return date in status format
    return time.toString();
  }

  /***************************************** changeValue *****************************************/
  @Override
  public void changeValue( double delta, boolean shift, boolean ctrl, boolean alt )
  {
    // modify field value
    if ( !shift && !ctrl )
      m_time.addMilliseconds( (int) ( delta * Time.ONE_HOUR ) );
    if ( shift && !ctrl )
      m_time.addMilliseconds( (int) ( delta * Time.ONE_MINUTE ) );
    if ( !shift && ctrl )
      m_time.addMilliseconds( (int) ( delta * Time.ONE_SECOND ) );

    // display in text and signal change
    setText( format( m_time ) );
    positionCaret( getText().length() );
    signal( m_time );
  }

}
