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

package rjc.table.view.cursor;

import javafx.scene.input.MouseEvent;

/*************************************************************************************************/
/****************** Interface for table-view mouse cursors with event handling *******************/
/*************************************************************************************************/

public interface ITableViewCursor
{
  /**************************************** handlePressed ****************************************/
  public void handlePressed( MouseEvent event ); // cursor specific button press handling

  /*************************************** handleReleased ****************************************/
  public void handleReleased( MouseEvent event ); // cursor specific button release handling

  /**************************************** handleClicked ****************************************/
  public void handleClicked( MouseEvent event ); // cursor specific button clicked handling

  /**************************************** handleDragged ****************************************/
  public void handleDragged( MouseEvent event ); // cursor specific mouse drag handling

  /**************************************** tableScrolled ****************************************/
  public void tableScrolled(); // cursor specific view scrolled handling

  /************************************* checkSelectPosition *************************************/
  public void checkSelectPosition(); // cursor specific select position setting
}
