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

import java.time.Month;
import java.time.YearMonth;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import rjc.table.data.types.Date;
import rjc.table.data.types.DateTime;
import rjc.table.data.types.Time;

/*************************************************************************************************/
/***************** Pop-up window to support selecting date or date-time or time ******************/
/*************************************************************************************************/

public class TEMP_DateTimeDropDown extends TEMP_DropDown
{
  private DateField        m_dateField;     // date field (or null if not)
  private TimeField        m_timeField;     // time field (or null if not)
  private DateTimeField    m_datetimeField; // date-time filed (or null if not)

  private MonthSpinField   m_monthField;
  private NumberSpinField  m_yearField;
  private CalendarWidget   m_calendar;
  private Button           m_todayButton;
  private TEMP_TimeWidget  m_timeWidget;

  private static final int BORDER = 4;

  // because GridPane getParent method is final need extra method to get parent field
  protected class GridField extends GridPane
  {
    public Parent getField()
    {
      if ( m_dateField != null )
        return m_dateField;
      if ( m_datetimeField != null )
        return m_datetimeField;
      return m_timeField;
    }
  }

  /**************************************** constructor ******************************************/
  public TEMP_DateTimeDropDown( ButtonField parent )
  {
    // create pop-up window and check parent is supported
    super( parent );
    if ( parent instanceof DateField )
      m_dateField = (DateField) parent;
    else if ( parent instanceof DateTimeField )
      m_datetimeField = (DateTimeField) parent;
    else if ( parent instanceof TimeField )
      m_timeField = (TimeField) parent;
    else
      throw new IllegalArgumentException( "Parent must be DateField, TimeField or DateTimeField " + parent.getClass() );

    // prepare grid for layout
    GridField grid = new GridField();
    grid.setHgap( BORDER );
    grid.setVgap( BORDER );
    grid.setPadding( new Insets( BORDER ) );
    getContent().add( grid );

    // when grid size changes ensure background size matches
    grid.widthProperty().addListener( x -> setBackgroundSize( grid.getWidth(), grid.getHeight() ) );
    grid.heightProperty().addListener( x -> setBackgroundSize( grid.getWidth(), grid.getHeight() ) );

    // prepare drop-down based on parent type
    if ( m_timeField != null )
    {
      // only show time fields
      m_timeWidget = new TEMP_TimeWidget( new CalendarWidget() );
      grid.add( m_timeWidget, 0, 0 );
      m_timeWidget.addListener( ( sender, time ) -> setTime( (Time) time[0] ) );
      m_timeField.addListener( ( sender, time ) -> setTime( (Time) time[0] ) );
    }
    else
    {
      // create the date widgets
      m_monthField = new MonthSpinField();
      m_yearField = new NumberSpinField();
      m_calendar = new CalendarWidget();
      m_todayButton = new Button( "Today" );

      // layout the date widgets
      grid.addRow( 0, m_monthField, m_yearField );
      grid.add( m_calendar, 0, 1, 2, 1 );
      grid.add( m_todayButton, 0, 2, 2, 1 );
      if ( m_datetimeField != null )
      {
        m_timeWidget = new TEMP_TimeWidget( m_calendar );
        grid.add( m_timeWidget, 0, 3, 2, 1 );
        m_timeWidget.addListener( ( sender, time ) -> setTime( (Time) time[0] ) );
      }

      // configure the date widgets
      int w = (int) ( m_calendar.getWidth() * 0.6 );
      m_monthField.setMaxWidth( w );
      m_monthField.setOverflowField( m_yearField );
      m_yearField.setMaxWidth( m_calendar.getWidth() - BORDER - w );
      m_yearField.setRange( 0, 5000 );
      m_todayButton.setPrefWidth( m_calendar.getWidth() );
      m_calendar.requestFocus();

      // listen to field changes
      if ( m_dateField != null )
        m_dateField.addListener( ( sender, date ) -> setDate( (Date) date[0] ) );
      if ( m_datetimeField != null )
        m_datetimeField.addListener( ( sender, datetime ) -> setDateTime( (DateTime) datetime[0] ) );

      // listen to widget changes
      m_yearField.addListener( ( sender, year ) -> setYear( ( (Double) year[0] ).intValue() ) );
      m_monthField.addListener( ( sender, month ) -> setMonth( (Month) month[0] ) );
      m_calendar.addListener( ( sender, date ) -> setDate( (Date) date[0] ) );
      m_todayButton.setOnAction( event -> setDate( Date.now() ) );

      // listen to mouse scroll wheel
      // grid.setOnScroll( event -> parent.mouseScroll( event ) );
    }
  }

  /******************************************* getDate *******************************************/
  public Date getDate()
  {
    // get date from calendar widget
    return m_calendar.getDate();
  }

  /******************************************* getTime *******************************************/
  public Time getTime()
  {
    // get time from time widget
    if ( m_timeWidget == null )
      return Time.MIN_VALUE;
    return m_timeWidget.getTime();
  }

  /****************************************** setDate ********************************************/
  private void setDate( Date date )
  {
    // set widgets to date
    m_monthField.setValue( date.getMonth() );
    m_yearField.setValue( date.getYear() );
    m_calendar.setDate( date );
    updateField();
  }

  /****************************************** setTime ********************************************/
  private void setTime( Time time )
  {
    // set time widget
    m_timeWidget.setTime( time );
    updateField();
  }

  /**************************************** setDateTime ******************************************/
  private void setDateTime( DateTime datetime )
  {
    // set widgets to date-time
    setDate( datetime.getDate() );
    setTime( datetime.getTime() );
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

  /***************************************** updateField *****************************************/
  private void updateField()
  {
    // update field text if drop-down showing - but run later to allow any field wrapping to complete first
    if ( isShowing() )
      Platform.runLater( () ->
      {
        if ( m_timeField != null )
          m_timeField.setTime( getTime() );
        if ( m_dateField != null )
          m_dateField.setDate( getDate() );
        if ( m_datetimeField != null )
          m_datetimeField.setDateTime( new DateTime( getDate(), getTime() ) );
      } );

  }

}