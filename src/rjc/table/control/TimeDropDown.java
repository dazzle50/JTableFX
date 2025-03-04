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

import javafx.application.Platform;
import rjc.table.data.types.Time;

/*************************************************************************************************/
/**************************** Pop-up window to support selecting time ****************************/
/*************************************************************************************************/

public class TimeDropDown extends DropDown
{
  private TimeField  m_timeField; // time field associated with this drop-down

  private TimeWidget m_timeWidget;

  /**************************************** constructor ******************************************/
  public TimeDropDown( TimeField timeField )
  {
    // create pop-up down-down box
    super( timeField );
    m_timeField = timeField;
    m_timeField.addListener( ( sender, time ) -> setTime( (Time) time[0] ) );

    m_timeWidget = new TimeWidget( m_timeField.getStatus() );
    getGrid().addRow( 0, m_timeWidget );
  }

  /******************************************* getDate *******************************************/
  public Time getTime()
  {
    // get date from calendar widget
    return m_timeWidget.getTime();
  }

  /****************************************** setDate ********************************************/
  protected void setTime( Time time )
  {
    // set widgets to date
    m_timeWidget.setTime( time );
    updateParent();
  }

  /**************************************** updateParent *****************************************/
  void updateParent()
  {
    // update field text if drop-down showing - but run later to allow any field wrapping to complete first
    if ( isShowing() )
      Platform.runLater( () -> m_timeField.setTime( getTime() ) );
  }

}
