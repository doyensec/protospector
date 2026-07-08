package doyensec.protospector.editor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;

import doyensec.protospector.grpc.blackbox.FieldTypePreviewer;
import doyensec.protospector.ui.UIUtils;

class ProtospectorEditorSchemaTable extends JScrollPane {
    private final FieldMutationHandler mutationHandler;
    private final JTable table;
    private final ProtoSchemaTableModel tableModel;

    ProtospectorEditorSchemaTable(FieldMutationHandler mutationHandler) {
        this.mutationHandler = mutationHandler;
        tableModel = new ProtoSchemaTableModel();
        table = new JTable(tableModel) {
            @Override
            public TableCellEditor getCellEditor(int row, int column) {
                if (column == ProtoSchemaTableModel.FIELD_TYPE_COLUMN) {
                    JComboBox<FieldTypeOption> typeComboBox = new JComboBox<>(
                            tableModel.getTypeOptions(convertRowIndexToModel(row)).toArray(new FieldTypeOption[0]));
                    return new DefaultCellEditor(typeComboBox);
                }
                return super.getCellEditor(row, column);
            }
        };
        table.setFillsViewportHeight(true);
        setViewportView(table);
    }

    void setSchema(Descriptor typeDescriptor, Message message) {
        tableModel.setSchema(typeDescriptor, message);
    }

    interface FieldMutationHandler {
        void updateFieldName(List<Integer> containingTypeIndexPath, int fieldIndex, String newName) throws Exception;

        void updateFieldType(List<Integer> containingTypeIndexPath, int fieldIndex,
                FieldDescriptorProto.Type type) throws Exception;
    }

    private final class ProtoSchemaTableModel extends AbstractTableModel {
        static final int FIELD_NAME_COLUMN = 0;
        static final int FIELD_TYPE_COLUMN = 1;

        private final String[] columns = { "Field Name (double-click to edit)", "Field Type (click to edit)" };
        private final List<SchemaFieldRow> rows = new ArrayList<>();
        private Descriptor rootDescriptor;

        void setSchema(Descriptor typeDescriptor, Message message) {
            rows.clear();
            rootDescriptor = typeDescriptor;
            if (typeDescriptor != null) {
                addRows(typeDescriptor, message, 0, new HashSet<>());
            }
            fireTableDataChanged();
        }

        private void addRows(Descriptor descriptor, Message parentMessage, int depth, Set<String> visitedDescriptors) {
            if (!visitedDescriptors.add(descriptor.getFullName())) {
                return;
            }
            for (FieldDescriptor field : descriptor.getFields()) {
                rows.add(new SchemaFieldRow(field, parentMessage, depth, getRelativeIndexPath(field.getContainingType())));
                if (field.getType() == FieldDescriptor.Type.MESSAGE || field.getType() == FieldDescriptor.Type.GROUP) {
                    addRows(field.getMessageType(), getChildMessage(parentMessage, field), depth + 1,
                            new HashSet<>(visitedDescriptors));
                }
            }
        }

        private Message getChildMessage(Message parentMessage, FieldDescriptor field) {
            if (parentMessage == null) {
                return null;
            }
            Object value;
            if (field.isRepeated()) {
                if (parentMessage.getRepeatedFieldCount(field) == 0) {
                    return null;
                }
                value = parentMessage.getRepeatedField(field, 0);
            } else {
                if (!parentMessage.hasField(field)) {
                    return null;
                }
                value = parentMessage.getField(field);
            }
            return value instanceof Message ? (Message) value : null;
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == FIELD_TYPE_COLUMN ? FieldTypeOption.class : String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            if (columnIndex == FIELD_NAME_COLUMN) {
                return true;
            }
            return columnIndex == FIELD_TYPE_COLUMN && rows.get(rowIndex).isTypeEditable();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            SchemaFieldRow row = rows.get(rowIndex);
            FieldDescriptor field = row.field;
            if (columnIndex == FIELD_NAME_COLUMN) {
                return row.getIndentedName();
            }
            return new FieldTypeOption(field.getType(), getPreview(row, field.getType()));
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            SchemaFieldRow row = rows.get(rowIndex);
            FieldDescriptor field = row.field;
            try {
                if (columnIndex == FIELD_NAME_COLUMN) {
                    String newName = value.toString().trim();
                    if (!newName.equals(field.getName())) {
                        mutationHandler.updateFieldName(row.relativeIndexPath, field.getIndex(), newName);
                    }
                } else if (columnIndex == FIELD_TYPE_COLUMN && value instanceof FieldTypeOption) {
                    FieldTypeOption option = (FieldTypeOption) value;
                    if (option.type != field.getType()) {
                        mutationHandler.updateFieldType(row.relativeIndexPath, field.getIndex(), option.type.toProto());
                    }
                }
            } catch (Exception e) {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    javax.swing.JOptionPane.showMessageDialog(
                        UIUtils.suiteFrame(),
                        "Error: " + e.getMessage(),
                        "Mutation Error",
                        javax.swing.JOptionPane.ERROR_MESSAGE
                    );
                });
           
                fireTableRowsUpdated(rowIndex, rowIndex);
            }
        }

        List<FieldTypeOption> getTypeOptions(int rowIndex) {
            SchemaFieldRow row = rows.get(rowIndex);
            List<FieldTypeOption> options = new ArrayList<>();
            for (FieldDescriptor.Type type : FieldDescriptor.Type.values()) {
                if (type == FieldDescriptor.Type.MESSAGE || type == FieldDescriptor.Type.GROUP) {
                    continue;
                }
                options.add(new FieldTypeOption(type, getPreview(row, type)));
            }
            return options;
        }

        private String getPreview(SchemaFieldRow row, FieldDescriptor.Type type) {
            try {
                Object preview = new FieldTypePreviewer(row.field, row.parentMessage).previewFieldType(type);
                return formatPreview(preview);
            } catch (IOException | IllegalArgumentException | ClassCastException | NullPointerException e) {
                return "PREVIEW FAILED";
            }
        }

        private String formatPreview(Object preview) {
            String previewText = String.valueOf(preview);
            if (previewText.length() > 80) {
                return previewText.substring(0, 77) + "...";
            }
            return previewText;
        }

        private List<Integer> getRelativeIndexPath(Descriptor descriptor) {
            if (rootDescriptor == null) {
                return null;
            }
            List<Integer> path = new ArrayList<>();
            Descriptor currentDescriptor = descriptor;
            while (currentDescriptor != null && currentDescriptor != rootDescriptor) {
                Descriptor parent = currentDescriptor.getContainingType();
                if (parent == null) {
                    return null;
                }
                path.add(currentDescriptor.getIndex());
                currentDescriptor = parent;
            }
            if (currentDescriptor != rootDescriptor) {
                return null;
            }
            Collections.reverse(path);
            return path;
        }
    }

    private static final class SchemaFieldRow {
        private final FieldDescriptor field;
        private final Message parentMessage;
        private final int depth;
        private final List<Integer> relativeIndexPath;

        SchemaFieldRow(FieldDescriptor field, Message parentMessage, int depth, List<Integer> relativeIndexPath) {
            this.field = field;
            this.parentMessage = parentMessage;
            this.depth = depth;
            this.relativeIndexPath = relativeIndexPath;
        }

        boolean isTypeEditable() {
            return relativeIndexPath != null
                    && field.getType() != FieldDescriptor.Type.MESSAGE
                    && field.getType() != FieldDescriptor.Type.GROUP;
        }

        String getIndentedName() {
            StringBuilder name = new StringBuilder();
            for (int i = 0; i < depth; i++) {
                name.append("    ");
            }
            name.append(field.getName());
            return name.toString();
        }
    }

    private static final class FieldTypeOption {
        private final FieldDescriptor.Type type;
        private final String preview;

        FieldTypeOption(FieldDescriptor.Type type, String preview) {
            this.type = type;
            this.preview = preview;
        }

        @Override
        public String toString() {
            return type.name() + " - " + preview;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof FieldTypeOption)) {
                return false;
            }
            FieldTypeOption other = (FieldTypeOption) obj;
            return type == other.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(type);
        }
    }
}
