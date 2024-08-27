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
import rjc.table.data.types.Date;
import rjc.table.signal.ISignal;
import rjc.table.signal.ObservableStatus;
import rjc.table.signal.ObservableStatus.Level;

/*************************************************************************************************/
/************************************** Date field control ***************************************/
/*************************************************************************************************/

public class DateField extends ButtonField implements ISignal
{
  private Date m_date; // field current date (or most recent valid)

  /**************************************** constructor ******************************************/
  public DateField( ObservableStatus status )
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
        // if no exception raised and date is different send signal (but don't update text)
        Date date = Date.parse( newText, "uuuu-MM-dd" );
        if ( !date.equals( m_date ) )
        {
          m_date = date;
          signal( date );
        }

        getStatus().update( Level.NORMAL, "Date: " + formatStatus( date ) );
        setStyle( getStatus().getStyle() );
      }
      catch ( Exception exception )
      {
        getStatus().update( Level.ERROR, "Date format is not recognised" );
        setStyle( getStatus().getStyle() );
      }
    } );

    // react to focus change to ensure text shows date in correct format
    focusedProperty().addListener( ( property, oldF, newF ) ->
    {
      setText( format( m_date ) );
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
        setText( format( m_date ) );
        positionCaret( getText().length() );
      }
    } );

    // set default date to today
    setDate( Date.now() );
  }

  /****************************************** getDate ********************************************/
  public Date getDate()
  {
    // return current field date (or most recent valid)
    return m_date;
  }

  /****************************************** setDate ********************************************/
  public void setDate( Date date )
  {
    // set current field date, display in text, signal change
    if ( !date.equals( m_date ) )
    {
      m_date = date;
      setText( format( date ) );
      positionCaret( getText().length() );
      signal( date );
    }
  }

  /******************************************* format ********************************************/
  public String format( Date date )
  {
    // return date in display format
    return date.toString();
  }

  /**************************************** formatStatus *****************************************/
  public String formatStatus( Date date )
  {
    // return date in status format
    return date.toString( "eeee d MMMM yyyy" );
  }

  /***************************************** changeValue *****************************************/
  @Override
  public void changeValue( double delta, boolean shift, boolean ctrl, boolean alt )
  {
    // modify field value
    if ( !shift && !ctrl )
      setDate( getDate().plusDays( (int) delta ) );
    if ( shift && !ctrl )
      setDate( getDate().plusMonths( (int) delta ) );
    if ( !shift && ctrl )
      setDate( getDate().plusYears( (int) delta ) );
  }

}