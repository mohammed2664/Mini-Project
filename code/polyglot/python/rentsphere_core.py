"""
RentSphere Core Engine - Python 3.12 Polyglot Reference Implementation
Author: Nikhil Bhaiju (Subsystem 3 Lead)
Institution: Goa University - MCA Object-Oriented Technology Lab
Currency: Indian Rupees (₹ INR)

Features:
 - 5-Tier Domain Model with Dynamic Introspection & Ground Bay Constraint
 - Prototype Deep vs Shallow Cloning Memory Verification (id() pointer analysis)
 - RentSphere Trust & Reliability Scoring Engine (Weighted Composite Metric)
"""

import copy
import inspect
import time
from typing import List, Dict, Any

# ============================================================================
# 5-TIER DOMAIN HIERARCHY
# ============================================================================

class AbstractDomainEntity:
    """Tier 1: Base Entity with Id and Timestamp"""
    def __init__(self, entity_id: str):
        self.id = entity_id
        self.created_at = int(time.time() * 1000)

    def serialize_to_pipe(self) -> str:
        raise NotImplementedError()

class RentableAsset(AbstractDomainEntity):
    """Tier 2: Rentable Asset with State and Maintenance Log Reference (₹ INR)"""
    def __init__(self, entity_id: str, title: str, owner_id: str, 
                 base_daily_rate: float, security_collateral: float, locker_coordinate: str):
        super().__init__(entity_id)
        self.title = title
        self.owner_id = owner_id
        self.renter_id = ""
        self.base_daily_rate = base_daily_rate          # ₹ INR
        self.security_collateral = security_collateral  # ₹ INR Refundable Deposit
        self.locker_coordinate = locker_coordinate      # e.g., "A2", "H2"
        self.state = "AVAILABLE"
        self.service_logs: List[str] = [
            "Factory Calibration Complete",
            "Safety Certification Audited"
        ]

    def calculate_daily_rental_tariff(self, rental_days: int) -> float:
        return self.base_daily_rate * rental_days

    def calculate_required_collateral(self) -> float:
        return self.security_collateral

    def calculate_damage_slashing_penalty(self, damage_pct: float) -> float:
        return self.security_collateral * (damage_pct / 100.0)

    def shallow_clone(self) -> "RentableAsset":
        return copy.copy(self)

    def deep_clone(self) -> "RentableAsset":
        return copy.deepcopy(self)

class CategorizedRentalAsset(RentableAsset):
    """Tier 3: Category Classification & Late Fee Multiplier"""
    def __init__(self, entity_id: str, title: str, owner_id: str,
                 base_daily_rate: float, security_collateral: float, locker_coordinate: str,
                 category: str, late_fee_multiplier: float, protection_plan_tier: str):
        super().__init__(entity_id, title, owner_id, base_daily_rate, security_collateral, locker_coordinate)
        self.category = category
        self.late_fee_multiplier = late_fee_multiplier
        self.protection_plan_tier = protection_plan_tier

class EventAndCreatorAsset(CategorizedRentalAsset):
    """Tier 4A: Event and Creator Equipment"""
    def __init__(self, entity_id: str, title: str, owner_id: str,
                 base_daily_rate: float, security_collateral: float, locker_coordinate: str,
                 category: str, late_fee_multiplier: float, protection_plan_tier: str,
                 indoor_outdoor_rating: str, wattage_or_battery_spec: str, accessory_kit_count: int):
        super().__init__(entity_id, title, owner_id, base_daily_rate, security_collateral, locker_coordinate,
                         category, late_fee_multiplier, protection_plan_tier)
        self.indoor_outdoor_rating = indoor_outdoor_rating
        self.wattage_or_battery_spec = wattage_or_battery_spec
        self.accessory_kit_count = accessory_kit_count

class UtilityAndMobilityAsset(CategorizedRentalAsset):
    """Tier 4B: Utility and Mobility Equipment"""
    def __init__(self, entity_id: str, title: str, owner_id: str,
                 base_daily_rate: float, security_collateral: float, locker_coordinate: str,
                 category: str, late_fee_multiplier: float, protection_plan_tier: str,
                 power_or_engine_spec: str, safety_certification: str, requires_ground_bay: bool):
        super().__init__(entity_id, title, owner_id, base_daily_rate, security_collateral, locker_coordinate,
                         category, late_fee_multiplier, protection_plan_tier)
        self.power_or_engine_spec = power_or_engine_spec
        self.safety_certification = safety_certification
        self.requires_ground_bay = requires_ground_bay

class CameraAndOpticsGear(EventAndCreatorAsset):
    """Tier 5 Concrete: DSLR/Mirrorless Cameras, Lenses & Gimbals"""
    def __init__(self, entity_id: str, title: str, owner_id: str,
                 base_daily_rate: float, security_collateral: float, locker_coordinate: str,
                 late_fee_multiplier: float, protection_plan_tier: str,
                 sensor_resolution_mp: float, is_4k_or_8k: bool):
        super().__init__(entity_id, title, owner_id, base_daily_rate, security_collateral, locker_coordinate,
                         "CAMERAS", late_fee_multiplier, protection_plan_tier,
                         "Studio/Outdoor", "Dual NP-FZ100", 4)
        self.sensor_resolution_mp = sensor_resolution_mp
        self.is_4k_or_8k = is_4k_or_8k

    def calculate_daily_rental_tariff(self, rental_days: int) -> float:
        resolution_factor = 1.15 if self.is_4k_or_8k else 1.0
        return self.base_daily_rate * resolution_factor * rental_days

    def calculate_required_collateral(self) -> float:
        return self.security_collateral * 1.20 if self.sensor_resolution_mp >= 30.0 else self.security_collateral

    def serialize_to_pipe(self) -> str:
        return f"{self.id}|{self.title}|CAMERAS|CameraAndOpticsGear|{self.owner_id}|{self.renter_id}|{self.locker_coordinate}|{self.base_daily_rate}"

class EventAndMobilityVehicle(UtilityAndMobilityAsset):
    """Tier 5 Concrete: Wedding/Event Cars & Campus Electric Scooters (Row H Ground Bay)"""
    def __init__(self, entity_id: str, title: str, owner_id: str,
                 base_daily_rate: float, security_collateral: float, locker_coordinate: str,
                 late_fee_multiplier: float, protection_plan_tier: str,
                 seating_capacity: int, fuel_or_charging_type: str):
        super().__init__(entity_id, title, owner_id, base_daily_rate, security_collateral, locker_coordinate,
                         "MOBILITY_VEHICLES", late_fee_multiplier, protection_plan_tier,
                         "1.2L Engine / Electric Motor", "ARAI 5-Star", True)
        self.seating_capacity = seating_capacity
        self.fuel_or_charging_type = fuel_or_charging_type

    def calculate_daily_rental_tariff(self, rental_days: int) -> float:
        return self.base_daily_rate * rental_days

    def calculate_required_collateral(self) -> float:
        return self.security_collateral * 1.20

    def serialize_to_pipe(self) -> str:
        return f"{self.id}|{self.title}|MOBILITY_VEHICLES|EventAndMobilityVehicle|{self.owner_id}|{self.renter_id}|{self.locker_coordinate}|{self.base_daily_rate}"

# ============================================================================
# DYNAMIC INTROSPECTION & PROTOTYPE CLONING BENCHMARK
# ============================================================================

def inspect_runtime_entity(entity: Any) -> Dict[str, Any]:
    """Uses Python RTTI / reflection inspection to expose class hierarchy & attributes"""
    mro_chain = [cls.__name__ for cls in inspect.getmro(entity.__class__)]
    attributes = {k: v for k, v in entity.__dict__.items() if not k.startswith("_")}
    methods = [name for name, _ in inspect.getmembers(entity, predicate=inspect.ismethod) if not name.startswith("_")]
    return {
        "class_name": entity.__class__.__name__,
        "mro_inheritance_chain": mro_chain,
        "attributes": attributes,
        "methods": methods
    }

def benchmark_cloning_memory(original: RentableAsset):
    """Proves difference between shallow and deep cloning via memory id() inspection"""
    shallow = original.shallow_clone()
    deep = original.deep_clone()

    print("\n--- Shallow vs Deep Cloning Memory Isolation Benchmark ---")
    print(f"Original Object ID:        {hex(id(original))}")
    print(f"Original Logs Reference ID: {hex(id(original.service_logs))}")
    print(f"Shallow Object ID:         {hex(id(shallow))} (Distinct Object)")
    print(f"Shallow Logs Reference ID: {hex(id(shallow.service_logs))} (SHARED Reference - Mutates Original)")
    print(f"Deep Object ID:            {hex(id(deep))} (Distinct Object)")
    print(f"Deep Logs Reference ID:    {hex(id(deep.service_logs))} (ISOLATED Reference - Safe for Multi-Unit)")

    # Mutate shallow clone
    shallow.service_logs.append("MUTATION_VIA_SHALLOW_CLONE")
    print(f"\nAfter appending to shallow clone:")
    print(f"Original Logs Count: {len(original.service_logs)} -> Contains 'MUTATION_VIA_SHALLOW_CLONE': {'MUTATION_VIA_SHALLOW_CLONE' in original.service_logs}")

    # Mutate deep clone
    deep.service_logs.append("MUTATION_VIA_DEEP_CLONE")
    print(f"After appending to deep clone:")
    print(f"Original Logs Count: {len(original.service_logs)} -> Contains 'MUTATION_VIA_DEEP_CLONE': {'MUTATION_VIA_DEEP_CLONE' in original.service_logs}")

# ============================================================================
# RENTSPHERE TRUST & RELIABILITY SCORING ENGINE (₹ INR)
# ============================================================================

class RentSphereTrustScoreEngine:
    """
    Weighted Composite Trust Metric:
    Score = (Condition * 0.30) + (On-Time * 0.30) + (Escrow * 0.20) + (Peer Rating * 0.10) + (KYC * 0.10)
    """
    WEIGHTS = {
        "CONDITION": 0.30,   # Asset Maintenance & Condition History
        "ON_TIME":   0.30,   # Punctuality and On-Time Return Ratio
        "ESCROW":    0.20,   # Successful Escrow Transaction Completion
        "RATING":    0.10,   # Peer Community Review Score
        "KYC":       0.10    # Identity & Hub Verification Tier
    }

    @staticmethod
    def calculate_trust_score(condition: float, on_time: float, escrow: float, 
                              rating: float, kyc: float) -> Dict[str, Any]:
        composite = (condition * 0.30) + (on_time * 0.30) + (escrow * 0.20) + (rating * 0.10) + (kyc * 0.10)
        
        tier = "Bronze Community Member"
        if composite >= 85.0:
            tier = "Diamond SuperHost / Elite Verified"
        elif composite >= 70.0:
            tier = "Gold Verified Trusted"
        elif composite >= 50.0:
            tier = "Silver Reliable Member"

        return {
            "condition": condition,
            "on_time": on_time,
            "escrow": escrow,
            "rating": rating,
            "kyc": kyc,
            "composite_score": round(composite, 2),
            "tier_badge": tier
        }

# ============================================================================
# PYTHON HARNESS
# ============================================================================
if __name__ == "__main__":
    print("=========================================================")
    print("  RentSphere Core Engine - Python 3.12 Polyglot Reference (₹ INR)")
    print("=========================================================")

    camera = CameraAndOpticsGear(
        "AST-001", "Sony Alpha A7 IV Mirrorless Camera + 28-70mm Kit", "host_faaris",
        1200.0, 5000.0, "A2", 1.25, "PREMIUM_ALL_RISK", 33.0, True
    )

    # 1. RTTI Introspection
    reflection_info = inspect_runtime_entity(camera)
    print("\n[RTTI INTROSPECTION] Class Hierarchy:")
    print(" -> ".join(reflection_info["mro_inheritance_chain"]))

    # 2. Cloning Benchmark
    benchmark_cloning_memory(camera)

    # 3. Trust Score Calculation
    print("\n--- RentSphere Trust & Reliability Score ---")
    trust_report = RentSphereTrustScoreEngine.calculate_trust_score(
        condition=92.0, on_time=95.0, escrow=88.0, rating=90.0, kyc=95.0
    )
    print(f"Mohammed Faaris Khan: Trust Score = {trust_report['composite_score']}/100 | Tier: {trust_report['tier_badge']}")
