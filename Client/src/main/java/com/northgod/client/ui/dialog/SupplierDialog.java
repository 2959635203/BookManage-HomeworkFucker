package com.northgod.client.ui.dialog;

import com.northgod.client.model.Supplier;

import javax.swing.*;
import java.awt.*;

public class SupplierDialog extends JDialog {
    private Supplier supplier;
    private boolean confirmed = false;

    private JTextField nameField;
    private JTextField contactPersonField;
    private JTextField contactPhoneField;
    private JTextField emailField;
    private JTextField addressField;
    private JTextField creditRatingField;
    private JTextField paymentTermsField;
    private JTextArea notesArea;

    public SupplierDialog(Frame parent, String title, Supplier supplier) {
        super(parent, title, true);
        this.supplier = supplier != null ? supplier : new Supplier();
        initComponents();
        pack();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        // 表单面板
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // 供应商名称（必填）- 添加星号标红
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel nameLabel = new JLabel("供应商名称:*");
        nameLabel.setForeground(Color.RED);
        formPanel.add(nameLabel, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        nameField = new JTextField(30);
        if (supplier.getName() != null) {
            nameField.setText(supplier.getName());
        }
        formPanel.add(nameField, gbc);

        // 联系人
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("联系人:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        contactPersonField = new JTextField(30);
        if (supplier.getContactPerson() != null) {
            contactPersonField.setText(supplier.getContactPerson());
        }
        formPanel.add(contactPersonField, gbc);

        // 联系电话
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("联系电话:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        contactPhoneField = new JTextField(30);
        if (supplier.getContactPhone() != null) {
            contactPhoneField.setText(supplier.getContactPhone());
        }
        formPanel.add(contactPhoneField, gbc);

        // 邮箱
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("邮箱:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        emailField = new JTextField(30);
        if (supplier.getEmail() != null) {
            emailField.setText(supplier.getEmail());
        }
        formPanel.add(emailField, gbc);

        // 地址
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("地址:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        addressField = new JTextField(30);
        if (supplier.getAddress() != null) {
            addressField.setText(supplier.getAddress());
        }
        formPanel.add(addressField, gbc);

        // 信用评级
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("信用评级:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        creditRatingField = new JTextField(30);
        if (supplier.getCreditRating() != null) {
            creditRatingField.setText(supplier.getCreditRating());
        }
        formPanel.add(creditRatingField, gbc);

        // 付款条款
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("付款条款:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        paymentTermsField = new JTextField(30);
        if (supplier.getPaymentTerms() != null) {
            paymentTermsField.setText(supplier.getPaymentTerms());
        }
        formPanel.add(paymentTermsField, gbc);

        // 备注
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("备注:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        notesArea = new JTextArea(5, 30);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        if (supplier.getNotes() != null) {
            notesArea.setText(supplier.getNotes());
        }
        JScrollPane notesScrollPane = new JScrollPane(notesArea);
        formPanel.add(notesScrollPane, gbc);

        JPanel formWrapper = new JPanel(new BorderLayout());
        formWrapper.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        formWrapper.add(formPanel, BorderLayout.CENTER);
        add(formWrapper, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("确定");
        JButton cancelButton = new JButton("取消");

        okButton.addActionListener(e -> {
            if (validateInput()) {
                confirmed = true;
                dispose();
            }
        });

        cancelButton.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        JPanel buttonWrapper = new JPanel(new BorderLayout());
        buttonWrapper.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buttonWrapper.add(buttonPanel, BorderLayout.EAST);
        add(buttonWrapper, BorderLayout.SOUTH);

        // 设置默认按钮
        getRootPane().setDefaultButton(okButton);
    }

    private boolean validateInput() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "供应商名称不能为空",
                    "验证错误",
                    JOptionPane.ERROR_MESSAGE);
            nameField.requestFocus();
            return false;
        }

        String email = emailField.getText().trim();
        if (!email.isEmpty() && !isValidEmail(email)) {
            JOptionPane.showMessageDialog(this,
                    "邮箱格式不正确",
                    "验证错误",
                    JOptionPane.ERROR_MESSAGE);
            emailField.requestFocus();
            return false;
        }

        return true;
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public Supplier getSupplier() {
        if (!confirmed) {
            return null;
        }

        Supplier result = new Supplier();
        result.setId(supplier.getId()); // 保留原ID（如果是编辑模式）
        result.setName(nameField.getText().trim());
        
        String contactPerson = contactPersonField.getText().trim();
        result.setContactPerson(contactPerson.isEmpty() ? null : contactPerson);
        
        String contactPhone = contactPhoneField.getText().trim();
        result.setContactPhone(contactPhone.isEmpty() ? null : contactPhone);
        
        String email = emailField.getText().trim();
        result.setEmail(email.isEmpty() ? null : email);
        
        String address = addressField.getText().trim();
        result.setAddress(address.isEmpty() ? null : address);
        
        String creditRating = creditRatingField.getText().trim();
        result.setCreditRating(creditRating.isEmpty() ? null : creditRating);
        
        String paymentTerms = paymentTermsField.getText().trim();
        result.setPaymentTerms(paymentTerms.isEmpty() ? null : paymentTerms);
        
        String notes = notesArea.getText().trim();
        result.setNotes(notes.isEmpty() ? null : notes);
        
        result.setIsActive(true); // 新添加的供应商默认为活跃

        return result;
    }
}

