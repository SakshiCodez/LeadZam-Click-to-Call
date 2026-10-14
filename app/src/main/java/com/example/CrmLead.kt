package com.example

/**
 * Data model for leads displayed in the in-app CRM Pipeline Dashboard.
 */
data class CrmLead(
    val id: String,
    val name: String,
    val role: String,
    val company: String,
    val phoneNumber: String,
    val stage: String,
    val dealValue: String,
    val initials: String,
    val avatarBgColorHex: String,
    val priority: String = "High Priority"
) {
    companion object {
        fun getSampleLeads(): List<CrmLead> {
            return listOf(
                CrmLead(
                    id = "lead-1",
                    name = "Rahul Sharma",
                    role = "VP of Operations",
                    company = "Apex Logistics Solutions",
                    phoneNumber = "+91 98765 43210",
                    stage = "Warm Lead",
                    dealValue = "₹1,80,000",
                    initials = "RS",
                    avatarBgColorHex = "#2563EB",
                    priority = "High Priority"
                ),
                CrmLead(
                    id = "lead-2",
                    name = "Priya Verma",
                    role = "Product Director",
                    company = "CloudMatrix Infotech",
                    phoneNumber = "+91 98765 12345",
                    stage = "Contract Sent",
                    dealValue = "₹3,20,000",
                    initials = "PV",
                    avatarBgColorHex = "#7C3AED",
                    priority = "Urgent Closing"
                ),
                CrmLead(
                    id = "lead-3",
                    name = "Amit Patel",
                    role = "Founder & CEO",
                    company = "TechSprint Mobile",
                    phoneNumber = "+91 98765 67890",
                    stage = "Discovery Call",
                    dealValue = "₹95,000",
                    initials = "AP",
                    avatarBgColorHex = "#0D9488",
                    priority = "Initial Touchpoint"
                ),
                CrmLead(
                    id = "lead-4",
                    name = "Sneha Reddy",
                    role = "Procurement Head",
                    company = "Horizon Retail Stores",
                    phoneNumber = "+91 98765 24680",
                    stage = "Qualified",
                    dealValue = "₹2,50,000",
                    initials = "SR",
                    avatarBgColorHex = "#16A34A",
                    priority = "Budget Approved"
                ),
                CrmLead(
                    id = "lead-5",
                    name = "Ananya Roy",
                    role = "Chief Marketing Officer",
                    company = "Elevate Digital Brands",
                    phoneNumber = "+91 98765 13579",
                    stage = "Follow-up",
                    dealValue = "₹1,40,000",
                    initials = "AR",
                    avatarBgColorHex = "#DB2777",
                    priority = "Awaiting Callback"
                ),
                CrmLead(
                    id = "lead-6",
                    name = "Vikram Malhotra",
                    role = "IT Infrastructure Lead",
                    company = "Surya Global Energy",
                    phoneNumber = "+91 98765 98765",
                    stage = "Negotiation",
                    dealValue = "₹4,50,000",
                    initials = "VM",
                    avatarBgColorHex = "#EA580C",
                    priority = "Enterprise Deal"
                )
            )
        }
    }
}
