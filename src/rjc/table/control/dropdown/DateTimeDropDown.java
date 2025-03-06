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

package rjc.table.control.dropdown;

import javafx.application.Platform;
import rjc.table.control.DateTimeField;
import rjc.table.control.TimeWidget;
import rjc.table.data.types.DateTime;

/*************************************************************************************************/
/************************ Pop-up window to support selecting date & time *************************/
/*************************************************************************************************/

public class DateTimeDropDown extends DateDropDown
{
  private DateTimeField m_dateTimeField; // date-time field associated with this drop-down

  private TimeWidget    m_timeWidget;

  /**************************************** constructor ******************************************/
  public DateTimeDropDown( DateTimeField dateTimeField )
  {
    // create pop-up down-down box
    super( dateTimeField );
    m_dateTimeField = dateTimeField;
    m_dateTimeField.addListener( ( sender, datetime ) -> setDateTime( (DateTime) datetime[0] ) );

    // add time widget to date-dropdown
    m_timeWidget = new TimeWidget( getCalender() );
    getGrid().add( m_timeWidget, 0, 3, 2, 1 );
  }

  /******************************************* getDate *******************************************/
  public DateTime getDateTime()
  {
    // return date-time shown in calendar and time widget
    return new DateTime( getDate(), m_timeWidget.getTime() );
  }

  /****************************************** setDate ********************************************/
  private void setDateTime( DateTime datetime )
  {
    // set widgets to date-time
    m_timeWidget.setTime( datetime.getTime() );
    setDate( datetime.getDate() );
  }

  /**************************************** updateParent *****************************************/
  @Override
  void updateParent()
  {
    // update field text if drop-down showing - but run later to allow any field wrapping to complete first
    if ( isShowing() )
      Platform.runLater( () -> m_dateTimeField.setDateTime( getDateTime() ) );
  }
}
