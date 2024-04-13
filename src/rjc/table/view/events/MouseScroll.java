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

package rjc.table.view.events;

import javafx.event.EventHandler;
import javafx.scene.input.ScrollEvent;
import rjc.table.view.TableView;

/*************************************************************************************************/
/************************** Handles mouse scroll events from table-view **************************/
/*************************************************************************************************/

public class MouseScroll implements EventHandler<ScrollEvent>
{

  /******************************************* handle ********************************************/
  @Override
  public void handle( ScrollEvent event )
  {
    // scroll up or down depending on mouse wheel scroll event
    var view = TableView.getEventView( event.getSource() );
    var scrollbar = view.getVerticalScrollBar();

    if ( scrollbar.isVisible() )
    {
      if ( event.getDeltaY() > 0 )
      {
        scrollbar.finishAnimation();
        scrollbar.decrement();
      }
      else
      {
        scrollbar.finishAnimation();
        scrollbar.increment();
      }
    }
  }

}