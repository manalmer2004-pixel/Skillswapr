package com.skillswap.app.data

import com.skillswap.app.R
import com.skillswap.app.data.model.*

object MockData {
    val SKILL_CATEGORIES = listOf(
        "All",
        "Water & Sanitation",
        "Farming",
        "Cooking & Baking",
        "Solar & Energy",
        "Health",
        "Mechanical"
    )

    val CURRENT_USER = UserProfile(
        id = "user_maya",
        name = "Maya Lin",
        handle = "@user",
        email = "maya@skillswapr.org",
        avatarUrl = "",
        location = "Nairobi, Kenya",
        distanceMiles = 0.0,
        bio = "Community organizer and practical skills volunteer. Passionate about clean water, food security, and sustainable local skill exchanges.",
        rating = 4.9,
        reviewCount = 18,
        timeCreditBalance = 3.5,
        totalHoursEarned = 8.0,
        totalHoursSpent = 4.5,
        joinedDate = "Member since Apr 2024",
        skillsOffered = listOf("Crop Rotation & Soil Health", "Basic First Aid Training", "Small Solar Panel Installation"),
        skillsWanted = listOf("Water Filter Maintenance", "Community Baking & Bread Basics", "Rainwater Collection Setup"),
        badges = listOf("Neighborhood Anchor", "10+ Hours Exchanged", "Top Rated")
    )

    val USER_CARLOS = UserProfile(
        id = "user_carlos",
        name = "Carlos Vance",
        handle = "@user_carlos",
        email = "carlos@skillswapr.org",
        avatarUrl = "",
        location = "Nairobi, Kenya · 0.6 km away",
        distanceMiles = 0.4,
        bio = "Community repair worker who keeps household tools and transport running. Looking to learn safe water-pump maintenance and solar basics.",
        rating = 4.95,
        reviewCount = 29,
        timeCreditBalance = 5.0,
        totalHoursEarned = 16.0,
        totalHoursSpent = 11.0,
        joinedDate = "Member since Jan 2024",
        skillsOffered = listOf("Water Filter Maintenance", "Community Baking & Bread Basics", "Small Solar Panel Installation"),
        skillsWanted = listOf("Basic First Aid Training", "Crop Rotation & Soil Health", "Water Filter Maintenance"),
        badges = listOf("Master Craftsman", "Fast Responder", "Founding Member")
    )

    val USER_ELENA = UserProfile(
        id = "user_elena",
        name = "Elena Rostova",
        handle = "@user_elena",
        email = "elena@skillswapr.org",
        avatarUrl = "",
        location = "Nairobi, Kenya · 1.3 km away",
        distanceMiles = 0.8,
        bio = "Smallholder farmer and community garden volunteer. I help neighbors improve harvests through healthy soil, water-wise growing, and practical food production.",
        rating = 4.88,
        reviewCount = 22,
        timeCreditBalance = 4.0,
        totalHoursEarned = 12.0,
        totalHoursSpent = 8.0,
        joinedDate = "Member since Mar 2024",
        skillsOffered = listOf("Crop Rotation & Soil Health", "Composting & Natural Fertilizer", "Water-Saving Irrigation"),
        skillsWanted = listOf("Water Filter Maintenance", "Basic First Aid Training", "Solar Battery Maintenance"),
        badges = listOf("Green Thumb", "Neighborhood Anchor")
    )

    val USER_DAVID = UserProfile(
        id = "user_david",
        name = "David Kim",
        handle = "@user_david",
        email = "david@skillswapr.org",
        avatarUrl = "",
        location = "Nairobi, Kenya · 1.9 km away",
        distanceMiles = 1.2,
        bio = "Community baker and food skills volunteer. Happy to teach affordable bread making and food preparation in exchange for health, water, or farming skills.",
        rating = 5.0,
        reviewCount = 14,
        timeCreditBalance = 2.0,
        totalHoursEarned = 9.0,
        totalHoursSpent = 7.0,
        joinedDate = "Member since Feb 2024",
        skillsOffered = listOf("Community Baking & Bread Basics", "Affordable Flatbread Making", "Food Preservation Basics"),
        skillsWanted = listOf("Small Solar Panel Installation", "Water Filter Maintenance", "Crop Rotation & Soil Health"),
        badges = listOf("Community Fixer", "5-Star Streak")
    )

    val USER_PRIYA = UserProfile(
        id = "user_priya",
        name = "Priya Sharma",
        handle = "@user_priya",
        email = "priya@skillswapr.org",
        avatarUrl = "",
        location = "Nairobi, Kenya · 2.7 km away",
        distanceMiles = 1.7,
        bio = "Community health volunteer and trainer. I teach practical first aid, hygiene, and prevention skills that families can use every day.",
        rating = 4.92,
        reviewCount = 31,
        timeCreditBalance = 6.5,
        totalHoursEarned = 24.0,
        totalHoursSpent = 17.5,
        joinedDate = "Member since Oct 2023",
        skillsOffered = listOf("Basic First Aid Training", "Community Hygiene & Safe Water", "Nutrition for Families"),
        skillsWanted = listOf("Community Baking & Bread Basics", "Small Solar Panel Installation", "Crop Rotation & Soil Health"),
        badges = listOf("Top Mentor", "Super Swapper")
    )

    val INITIAL_LISTINGS = listOf(
        SkillListing(
            id = "listing_wood_shelves",
            userId = "user_carlos",
            type = "offer",
            title = "Water Filter Maintenance",
            category = "Water & Sanitation",
            description = "Learn how to clean, assemble, and maintain household bio-sand and ceramic water filters for clean drinking water.",
            imageResId = R.drawable.water_repair_maintenance,
            estimatedHours = 2.0,
            creditCost = 2.0,
            locationArea = "Nairobi, Kenya",
            distanceMiles = 0.4,
            availability = "Saturdays 10 AM - 2 PM",
            toolsProvided = listOf("Clean Containers", "Replacement Filter Media", "Brushes", "Basic Hand Tools"),
            createdAt = "2 days ago",
            teacherName = "Carlos Vance",
            teacherAvatar = "",
            teacherRating = 4.95,
            saved = false
        ),
        SkillListing(
            id = "listing_garden_setup",
            userId = "user_elena",
            type = "offer",
            title = "Crop Rotation & Soil Health",
            category = "Farming",
            description = "Practical guide on composting, testing soil pH naturally, and structuring crop rotation for higher yields.",
            imageResId = R.drawable.crop_and_soil_health,
            estimatedHours = 2.0,
            creditCost = 2.0,
            locationArea = "Nairobi, Kenya",
            distanceMiles = 0.8,
            availability = "Sundays after 2 PM",
            toolsProvided = listOf("Soil Jar Test", "Hand Hoe", "Compost Samples", "Planting Calendar"),
            createdAt = "3 days ago",
            teacherName = "Elena Rostova",
            teacherAvatar = "",
            teacherRating = 4.88,
            saved = true
        ),
        SkillListing(
            id = "listing_bike_tuneup",
            userId = "user_david",
            type = "offer",
            title = "Community Baking & Bread Basics",
            category = "Cooking & Baking",
            description = "Hands-on session on yeast dough, kneading, flatbreads, and baking delicious bread using affordable local ingredients.",
            imageResId = R.drawable.baking,
            estimatedHours = 1.5,
            creditCost = 1.5,
            locationArea = "Nairobi, Kenya",
            distanceMiles = 1.2,
            availability = "Weekday evenings",
            toolsProvided = listOf("Baking Sheet", "Mixing Bowls", "Measuring Cups", "Ingredients"),
            createdAt = "5 days ago",
            teacherName = "David Kim",
            teacherAvatar = "",
            teacherRating = 5.0,
            saved = false
        ),
        SkillListing(
            id = "listing_solar_installation",
            userId = "user_maya",
            type = "offer",
            title = "Small Solar Panel Installation",
            category = "Solar & Energy",
            description = "Learn how to wire 12V DC solar panels, charge controllers, and battery backups safely for emergency power.",
            imageResId = R.drawable.solar_panel_installation,
            estimatedHours = 2.0,
            creditCost = 2.0,
            locationArea = "Nairobi, Kenya",
            distanceMiles = 0.0,
            availability = "Weekends",
            toolsProvided = listOf("Wire Strippers", "Multimeter", "Mounting Brackets", "Safety Gloves"),
            createdAt = "1 week ago",
            teacherName = "Maya Lin",
            teacherAvatar = "",
            teacherRating = 4.9,
            saved = false
        ),
        SkillListing(
            id = "listing_spanish_chat",
            userId = "user_priya",
            type = "offer",
            title = "Basic First Aid Training",
            category = "Health",
            description = "Essential first responder skills for wound care, CPR basics, bandaging, and managing household health emergencies.",
            imageResId = R.drawable.first_aid,
            estimatedHours = 1.0,
            creditCost = 1.0,
            locationArea = "Nairobi, Kenya",
            distanceMiles = 1.7,
            availability = "Flexible / Remote",
            toolsProvided = listOf("First Aid Kit", "Clean Bandages", "Soap", "Training Cards"),
            createdAt = "1 week ago",
            teacherName = "Priya Sharma",
            teacherAvatar = "",
            teacherRating = 4.92,
            saved = true
        ),
        SkillListing(
            id = "listing_req_tax",
            userId = "user_carlos",
            type = "request",
            title = "Seeking: Clean Water Storage Advice",
            category = "Water & Sanitation",
            description = "Need guidance on safely storing rainwater and treating household storage tanks against algae and mosquitoes.",
            imageResId = R.drawable.water_storage_device,
            estimatedHours = 3.0,
            creditCost = 3.0,
            locationArea = "Nairobi, Kenya",
            distanceMiles = 0.4,
            availability = "Flexible",
            toolsProvided = listOf("Covered Containers", "Soap", "Water Treatment Notes", "Notebook"),
            createdAt = "Yesterday",
            teacherName = "Carlos Vance",
            teacherAvatar = "",
            teacherRating = 4.95,
            saved = false
        )
    )

    val INITIAL_REVIEWS = listOf(
        ReviewItem(
            id = "rev_1",
            reviewerId = "user_priya",
            reviewerName = "Priya Sharma",
            reviewerAvatar = "",
            rating = 5.0,
            comment = "Maya helped me revitalize my garden with organic composting! She brought her own soil mix and seedling starter kit. Truly what neighborly community is all about.",
            skillName = "Crop Rotation & Soil Health",
            date = "3 days ago"
        ),
        ReviewItem(
            id = "rev_2",
            reviewerId = "user_david",
            reviewerName = "David Kim",
            reviewerAvatar = "",
            rating = 5.0,
            comment = "Maya made solar panel setup feel simple and safe. We connected the charge controller, tested the battery, and now I can keep essential lights powered.",
            skillName = "Small Solar Panel Installation",
            date = "1 week ago"
        ),
        ReviewItem(
            id = "rev_3",
            reviewerId = "user_carlos",
            reviewerName = "Carlos Vance",
            reviewerAvatar = "",
            rating = 5.0,
            comment = "Great communication and prompt swap. Maya taught me the basic first aid and emergency response procedures.",
            skillName = "Basic First Aid Training",
            date = "2 weeks ago"
        )
    )

    val INITIAL_THREADS = listOf(
        RequestThread(
            id = "thread_carlos_shelves",
            listingId = "listing_wood_shelves",
            listingTitle = "Water Filter Maintenance",
            listingCategory = "Water & Sanitation",
            otherUser = USER_CARLOS,
            status = "pending",
            creditsProposed = 2.0,
            proposedDate = "Saturday, 10:00 AM",
            lastMessage = "Hey Maya! I saw your request for water filter maintenance. I have my tools ready for this weekend.",
            lastMessageTime = "10:42 AM",
            unreadCount = 1,
            messages = listOf(
                ChatMessage("m1", "user_carlos", "Hi Maya! Thanks for reaching out. I'd love to help maintain your ceramic water filters.", "10:35 AM"),
                ChatMessage("m2", "user_maya", "Hi Carlos! That would be amazing. Will 2 hours (2 credits) work for you?", "10:38 AM"),
                ChatMessage("m3", "user_carlos", "Hey Maya! I saw your request for water filter maintenance. I have my tools ready for this weekend.", "10:42 AM")
            )
        ),
        RequestThread(
            id = "thread_elena_garden",
            listingId = "listing_garden_setup",
            listingTitle = "Crop Rotation & Soil Health",
            listingCategory = "Farming",
            otherUser = USER_ELENA,
            status = "accepted",
            creditsProposed = 2.0,
            proposedDate = "Sunday, 2:00 PM",
            lastMessage = "Sounds good! See you on Sunday at 2pm. I'll bring the soil notes and planting calendar.",
            lastMessageTime = "Yesterday 4:02 PM",
            unreadCount = 0,
            messages = listOf(
                ChatMessage("em1", "user_maya", "Hi Elena! Would you be free to inspect my back patio soil this Sunday?", "Yesterday 3:15 PM"),
                ChatMessage("em2", "user_elena", "Sounds good! See you on Sunday at 2pm. I'll bring the soil notes and planting calendar.", "Yesterday 4:02 PM")
            )
        ),
        RequestThread(
            id = "thread_david_bike",
            listingId = "listing_bike_tuneup",
            listingTitle = "Community Baking & Bread Basics",
            listingCategory = "Cooking & Baking",
            otherUser = USER_DAVID,
            status = "completed",
            creditsProposed = 1.5,
            proposedDate = "Completed Sep 18",
            lastMessage = "Skill exchange completed: 1.5 Time Credits were deposited into User's balance.",
            lastMessageTime = "Sep 18 6:00 PM",
            unreadCount = 0,
            messages = listOf(
                ChatMessage("dm1", "system", "Skill exchange completed: 1.5 Time Credits were deposited into User's balance.", "Sep 18 6:00 PM", isSystem = true)
            )
        )
    )
}
