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

module JTableFX
{
  requires transitive javafx.base;
  requires transitive javafx.graphics;
  requires transitive javafx.controls;

  exports rjc.table;
  exports rjc.table.control;
  exports rjc.table.control.dropdown;
  exports rjc.table.data;
  exports rjc.table.data.types;
  exports rjc.table.demo;
  exports rjc.table.signal;
  exports rjc.table.undo;
  exports rjc.table.undo.commands;
  exports rjc.table.view;
  exports rjc.table.view.action;
  exports rjc.table.view.axis;
  exports rjc.table.view.cell;
  exports rjc.table.view.editor;
}
