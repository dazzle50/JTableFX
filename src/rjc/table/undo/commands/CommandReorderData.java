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

package rjc.table.undo.commands;

import java.util.Set;

import javafx.geometry.Orientation;
import rjc.table.data.TableData;
import rjc.table.undo.IUndoCommand;

/*************************************************************************************************/
/******************* UndoCommand for reordering columns or rows in table-data ********************/
/*************************************************************************************************/
public class CommandReorderData implements IUndoCommand
{

  /**************************************** constructor ******************************************/
  public CommandReorderData( TableData data, Orientation orientation, Set<Integer> selected, int insertIndex )
  {
    // prepare reorder command
  }

  /******************************************* redo **********************************************/
  @Override
  public void redo()
  {
  }

  /******************************************* undo **********************************************/
  @Override
  public void undo()
  {
  }

  /******************************************* text **********************************************/
  @Override
  public String text()
  {
    return null;
  }

}
