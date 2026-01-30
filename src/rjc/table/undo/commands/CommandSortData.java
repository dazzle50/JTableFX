/**************************************************************************
 *  Copyright (C) 2026 by Richard Crook                                   *
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

import rjc.table.Utils;
import rjc.table.undo.IUndoCommand;
import rjc.table.view.TableView;
import rjc.table.view.action.Sort.SortType;

public class CommandSortData implements IUndoCommand
{

  public CommandSortData( TableView view, int viewColumn, SortType type )
  {
    // TODO Auto-generated constructor stub
    Utils.trace( "NOT YET IMPLEMENTED" );
  }

  @Override
  public void redo()
  {
    // TODO Auto-generated method stub
    Utils.trace( "NOT YET IMPLEMENTED" );
  }

  @Override
  public void undo()
  {
    // TODO Auto-generated method stub
    Utils.trace( "NOT YET IMPLEMENTED" );
  }

  @Override
  public String text()
  {
    // TODO Auto-generated method stub
    return null;
  }
}
