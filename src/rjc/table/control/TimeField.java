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

package rjc.table.control;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import rjc.table.control.dropdown.TimeDropDown;
import rjc.table.data.types.Time;
import rjc.table.signal.ISignal;
import rjc.table.signal.ObservableStatus;
import rjc.table.signal.ObservableStatus.Level;

/*************************************************************************************************/
/************************************** Time field control ***************************************/
/*************************************************************************************************/

public class TimeField extends ButtonField implements ISignal
{
  private Time m_time; // field current time (or most recent valid)

  /**************************************** constructor ******************************************/
  public TimeField()
  {
    // construct field
    setButtonType( ButtonType.DOWN );
    new TimeDropDown( this );

    // react to changes & key presses
    textProperty().addListener( ( property, oldText, newText ) -> parseText( newText ) );
    addEventFilter( KeyEvent.KEY_PRESSED, event -> keyPressed( event ) );

    focusedProperty().addListener( ( property, oldF, newF ) ->
    {
      if ( newF )
        updateStatus( Level.NORMAL );
      else
        validText();
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
  public void setTime( Object value )
  {
    // set field value depending on object type
    if ( value instanceof Time time )
      setTime( time );
    else
    {
      String txt = value == null ? "" : value.toString();
      setText( txt );
      positionCaret( getText().length() );
    }
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

  /***************************************** parseText *******************************************/
  private void parseText( String newText )
  {
    // check if string can be parsed as a time, and update status
    try
    {
      // if no exception raised and time is different send signal (but don't update text)
      Time time = Time.fromString( newText );
      if ( !time.equals( m_time ) )
      {
        m_time = time;
        signal( time );
      }

      updateStatus( Level.NORMAL );
    }
    catch ( Exception exception )
    {
      updateStatus( Level.ERROR );
    }

  }

  /**************************************** updateStatus *****************************************/
  private void updateStatus( Level level )
  {
    // update status with level and appropriate text
    if ( getStatus() != null )
    {
      String msg = level == Level.NORMAL ? "Time: " + formatStatus( m_time ) : "Time format is not recognised";
      getStatus().update( level, msg );
    }
    setStyle( ObservableStatus.getStyle( level ) );
  }

  /***************************************** validText *******************************************/
  private void validText()
  {
    // ensure field displays last valid date
    setText( format( m_time ) );
    positionCaret( getText().length() );
    getStatus().clear();
  }

  /***************************************** keyPressed ******************************************/
  @Override
  public void keyPressed( KeyEvent event )
  {
    // react to certain key presses
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
      validText();
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
