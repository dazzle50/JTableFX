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

import java.lang.ref.WeakReference;
import java.time.Month;
import java.time.YearMonth;

import javafx.scene.control.Button;
import rjc.table.control.dropdown.AbstractDropDownField;
import rjc.table.control.dropdown.DropDown;
import rjc.table.data.types.Date;
import rjc.table.signal.ObservableStatus.Level;

/*************************************************************************************************/
/******************************* Date field control with drop-down *******************************/
/*************************************************************************************************/

public class DateField extends AbstractDropDownField
{
  protected Date          m_date;       // field current date (or most recent valid)

  private MonthSpinField  m_monthField;
  private NumberSpinField m_yearField;
  private CalendarWidget  m_calendar;
  private Button          m_todayButton;

  /**************************************** constructor ******************************************/
  public DateField()
  {
    // create the date widgets for the drop-down
    m_monthField = new MonthSpinField();
    m_yearField = new NumberSpinField();
    m_calendar = new CalendarWidget();
    m_todayButton = new Button( "Today" );

    // layout the date widgets
    getDropDownGrid().addRow( 0, m_monthField, m_yearField );
    getDropDownGrid().add( m_calendar, 0, 1, 2, 1 );
    getDropDownGrid().add( m_todayButton, 0, 2, 2, 1 );

    // configure the date widgets
    int w = (int) ( m_calendar.getWidth() * 0.6 );
    m_monthField.setMaxWidth( w );
    m_monthField.setOverflowField( m_yearField );
    m_yearField.setMaxWidth( m_calendar.getWidth() - DropDown.GRID_BORDER - w );
    m_yearField.setRange( 0, 5000 );
    m_yearField.setFormat( "0", 6, 0 );
    m_todayButton.setPrefWidth( m_calendar.getWidth() );
    m_calendar.requestFocus();

    // listen to widget changes
    var weak = new WeakReference<DateField>( this );
    m_yearField.addListener( ( sender, year ) -> weak.get().setYear( ( (Double) year[0] ).intValue() ) );
    m_monthField.addListener( ( sender, month ) -> weak.get().setMonth( (Month) month[0] ) );
    m_calendar.addListener( ( sender, date ) -> weak.get().setDate( (Date) date[0] ) );
    m_todayButton.setOnAction( event -> weak.get().setDate( Date.now() ) );

    // set default date to today
    setDate( Date.now() );
  }

  /************************************ updateDropDownWidgets ************************************/
  @Override
  protected void updateDropDownWidgets()
  {
    // update the widgets in drop-down to reflect current date
    m_calendar.setDate( m_date );
    m_monthField.setValue( m_date.getMonth() );
    m_yearField.setValue( m_date.getYear() );
  }

  /****************************************** getDate ********************************************/
  public Date getDate()
  {
    // return current field date (or most recent valid)
    return m_date;
  }

  /****************************************** setDate ********************************************/
  public void setDate( Object value )
  {
    // set field value depending on object type
    if ( value instanceof Date date )
      setDate( date );
    else
    {
      String txt = value == null ? "" : value.toString();
      setText( txt );
    }
  }

  /****************************************** setDate ********************************************/
  public void setDate( Date date )
  {
    // set current field date, display in text, signal change
    if ( !date.equals( m_date ) )
    {
      m_date = date;
      setText( format( date ) );
      updateDropDownWidgets();
      signal( date );
    }
  }

  /****************************************** setMonth *******************************************/
  private void setMonth( Month month )
  {
    // change calendar widget month - ensuring day is valid
    Date date = m_calendar.getDate();
    int year = date.getYear();
    int day = date.getDayOfMonth();
    YearMonth ym = YearMonth.of( year, month );
    if ( day > ym.lengthOfMonth() )
      day = ym.lengthOfMonth();

    m_calendar.setDate( new Date( year, month.getValue(), day ) );
  }

  /******************************************* setYear *******************************************/
  private void setYear( int year )
  {
    // change calendar widget year - ensuring day is valid
    Date date = m_calendar.getDate();
    int month = date.getMonth();
    int day = date.getDayOfMonth();
    YearMonth ym = YearMonth.of( year, month );
    if ( day > ym.lengthOfMonth() )
      day = ym.lengthOfMonth();

    m_calendar.setDate( new Date( year, month, day ) );
  }

  /******************************************* format ********************************************/
  private String format( Date date )
  {
    // return date in display format
    return date.toString();
  }

  /**************************************** formatStatus *****************************************/
  private String formatStatus( Date date )
  {
    // return date in status format
    return date.toString( "eeee d MMMM yyyy" );
  }

  /***************************************** parseText *******************************************/
  @Override
  protected void parseText( String text )
  {
    // convert text to date, and if different signal (any exception handled in abstract)
    Date date = Date.parse( text, "uuuu-MM-dd" );
    if ( !date.equals( m_date ) )
    {
      m_date = date;
      signal( date );
    }
  }

  /***************************************** statusText ******************************************/
  @Override
  protected String statusText( Level level )
  {
    // return status text appropriate to the level
    return level == Level.NORMAL ? "Date: " + formatStatus( m_date ) : "Date format is not recognised";
  }

  /***************************************** validText *******************************************/
  @Override
  protected void validText()
  {
    // ensure field displays last valid date
    setText( format( m_date ) );
    getStatus().clear();
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
