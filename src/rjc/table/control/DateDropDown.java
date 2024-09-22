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
import javafx.scene.control.Button;
import rjc.table.data.types.Date;

/*************************************************************************************************/
/**************************** Pop-up window to support selecting date ****************************/
/*************************************************************************************************/

public class DateDropDown extends DropDown
{
  private DateField       m_dateField;  // date field

  private MonthSpinField  m_monthField;
  private NumberSpinField m_yearField;
  private CalendarWidget  m_calendar;
  private Button          m_todayButton;

  /**************************************** constructor ******************************************/
  public DateDropDown( DateField dateField )
  {
    // create pop-up down-down box
    super( dateField );
    m_dateField = dateField;

    // create the date widgets
    m_monthField = new MonthSpinField();
    m_yearField = new NumberSpinField();
    m_calendar = new CalendarWidget();
    m_todayButton = new Button( "Today" );

    // layout the date widgets
    getGrid().addRow( 0, m_monthField, m_yearField );
    getGrid().add( m_calendar, 0, 1, 2, 1 );
    getGrid().add( m_todayButton, 0, 2, 2, 1 );

    // configure the date widgets
    int w = (int) ( m_calendar.getWidth() * 0.6 );
    m_monthField.setMaxWidth( w );
    m_monthField.setOverflowField( m_yearField );
    m_yearField.setMaxWidth( m_calendar.getWidth() - GRID_BORDER - w );
    m_yearField.setRange( 0, 5000 );
    m_todayButton.setPrefWidth( m_calendar.getWidth() );
    m_calendar.requestFocus();

    // listen to parent changes
    m_dateField.addListener( ( sender, date ) -> setDate( (Date) date[0] ) );

    // listen to widget changes
    m_yearField.addListener( ( sender, year ) -> setYear( ( (Double) year[0] ).intValue() ) );
    m_monthField.addListener( ( sender, month ) -> setMonth( (Month) month[0] ) );
    m_calendar.addListener( ( sender, date ) -> setDate( (Date) date[0] ) );
    m_todayButton.setOnAction( event -> setDate( Date.now() ) );
  }

  /******************************************* getDate *******************************************/
  public Date getDate()
  {
    // get date from calendar widget
    return m_calendar.getDate();
  }

  /****************************************** setDate ********************************************/
  private void setDate( Date date )
  {
    // set widgets to date
    m_monthField.setValue( date.getMonth() );
    m_yearField.setValue( date.getYear() );
    m_calendar.setDate( date );
    updateParent();
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

  /**************************************** updateParent *****************************************/
  private void updateParent()
  {
    // update field text if drop-down showing - but run later to allow any field wrapping to complete first
    if ( isShowing() )
      Platform.runLater( () -> m_dateField.setDate( getDate() ) );
  }

}
