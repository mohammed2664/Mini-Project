"""
RentSphere Core Engine - Python 3.12 Polyglot Reference Implementation
Author: Nikhil Bhaiju (Subsystem 3 Lead)
Institution: Goa University - MCA Object-Oriented Technology Lab

Features:
 - 5-Tier Domain Model with Dynamic Introspection
 - Deep vs Shallow Cloning Memory Verification (id() analysis)
 - Ministry of Education NIRF Composite Score Calculation Engine
"""

import copy
import inspect
import time
from typing import List, Dict, Any

# ============================================================================
# ASSIGNMENT 2: 5-TIER DOMAIN HIERARCHY
# ============================================================================

class AbstractDomainEntity:
    """Tier 1: Base Entity with Id and Timestamp"""
    def __init__(self, entity_id: str):
        self.id = entity_id
        self.created_at = int(time.time() * 1000)

    def serialize_to_pipe(self) -> str:
        raise NotImplementedError()

class RentableAsset(AbstractDomainEntity):
    """Tier 2: Rentable Asset with State and Maintenance Log Reference"""
    def __init__(self, entity_id: str, title: str, owner_id: str, 
                 base_daily_rate: float, security_collateral: float, locker_coordinate: str):
        super().__init__(entity_id)
        self.title = title
        self.owner_id = owner_id
        self.renter_id = ""
        self.base_daily_rate = base_daily_rate
        self.security_collateral = security_collateral
        self.locker_coordinate = locker_coordinate
        self.state = "AVAILABLE"
        self.maintenance_logs: List[str] = [
            "Factory Calibration Complete",
            "Initial Firmware v1.0.0 Flashed"
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

class SectoralAsset(RentableAsset):
    """Tier 3: Sector Classification (Tourism vs Agriculture)"""
    def __init__(self, entity_id: str, title: str, owner_id: str,
                 base_daily_rate: float, security_collateral: float, locker_coordinate: str,
                 sector: str, seasonal_multiplier: float):
        super().__init__(entity_id, title, owner_id, base_daily_rate, security_collateral, locker_coordinate)
        self.sector = sector
        self.seasonal_demand_multiplier = seasonal_multiplier

    def calculate_daily_rental_tariff(self, rental_days: int) -> float:
        return self.base_daily_rate * self.seasonal_demand_multiplier * rental_days

class TourismAsset(SectoralAsset):
    """Tier 4: Tourism Specialized Gear"""
    def __init__(self, entity_id: str, title: str, owner_id: str,
                 base_daily_rate: float, security_collateral: float, locker_coordinate: str,
                 seasonal_multiplier: float, ip_rating: str, corrosion_factor: float):
        super().__init__(entity_id, title, owner_id, base_daily_rate, security_collateral, locker_coordinate, "TOURISM", seasonal_multiplier)
        self.ip_rating = ip_rating
        self.saltwater_corrosion_factor = corrosion_factor

class AutonomousMarineDrone(TourismAsset):
    """Tier 5: Concrete Leaf Drone Class"""
    def __init__(self, entity_id: str, title: str, owner_id: str,
                 base_daily_rate: float, security_collateral: float, locker_coordinate: str,
                 seasonal_multiplier: float, ip_rating: str, corrosion_factor: float,
                 max_depth_meters: float, sonar_equipped: bool):
        super().__init__(entity_id, title, owner_id, base_daily_rate, security_collateral, locker_coordinate, seasonal_multiplier, ip_rating, corrosion_factor)
        self.max_depth_meters = max_depth_meters
        self.sonar_equipped = sonar_equipped

    def calculate_daily_rental_tariff(self, rental_days: int) -> float:
        depth_surcharge = 1.0 + (self.max_depth_meters / 500.0)
        return self.base_daily_rate * self.seasonal_demand_multiplier * depth_surcharge * rental_days

    def serialize_to_pipe(self) -> str:
        return f"{self.id}|{self.title}|TOURISM|AutonomousMarineDrone|{self.owner_id}|{self.renter_id}|{self.locker_coordinate}|{self.base_daily_rate}"

# ============================================================================
# ASSIGNMENT 4: DYNAMIC INTROSPECTION & CLONING BENCHMARK
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
    print(f"Original Logs Reference ID: {hex(id(original.maintenance_logs))}")
    print(f"Shallow Object ID:         {hex(id(shallow))} (Distinct Object)")
    print(f"Shallow Logs Reference ID: {hex(id(shallow.maintenance_logs))} (SHARED Reference - Shallow)")
    print(f"Deep Object ID:            {hex(id(deep))} (Distinct Object)")
    print(f"Deep Logs Reference ID:    {hex(id(deep.maintenance_logs))} (ISOLATED Reference - Deep)")

    # Mutate shallow clone
    shallow.maintenance_logs.append("MUTATION_VIA_SHALLOW_CLONE")
    print(f"\nAfter appending to shallow clone:")
    print(f"Original Logs Count: {len(original.maintenance_logs)} -> Contains 'MUTATION_VIA_SHALLOW_CLONE': {'MUTATION_VIA_SHALLOW_CLONE' in original.maintenance_logs}")

    # Mutate deep clone
    deep.maintenance_logs.append("MUTATION_VIA_DEEP_CLONE")
    print(f"After appending to deep clone:")
    print(f"Original Logs Count: {len(original.maintenance_logs)} -> Contains 'MUTATION_VIA_DEEP_CLONE': {'MUTATION_VIA_DEEP_CLONE' in original.maintenance_logs}")

# ============================================================================
# ASSIGNMENT 4: NIRF INSTITUTIONAL ANALYTICS ENGINE
# ============================================================================

class NirfCompositeScoreCalculator:
    """
    Ministry of Education NIRF Composite Score Formula:
    Score = (TLR * 0.30) + (RP * 0.30) + (GO * 0.20) + (OI * 0.10) + (PR * 0.10)
    """
    WEIGHTS = {
        "TLR": 0.30,  # Teaching, Learning & Resources
        "RP":  0.30,  # Research and Professional Practice
        "GO":  0.20,  # Graduation Outcomes
        "OI":  0.10,  # Outreach and Inclusivity
        "PR":  0.10   # Peer Perception
    }

    @staticmethod
    def calculate_score(tlr: float, rp: float, go: float, oi: float, pr: float) -> Dict[str, Any]:
        composite = (tlr * 0.30) + (rp * 0.30) + (go * 0.20) + (oi * 0.10) + (pr * 0.10)
        
        tier = "Tier 3 Regional"
        if composite >= 75.0:
            tier = "Top 10 National Excellence"
        elif composite >= 60.0:
            tier = "Tier 1 High Performance"
        elif composite >= 45.0:
            tier = "Tier 2 Promising Institution"

        return {
            "tlr": tlr,
            "rp": rp,
            "go": go,
            "oi": oi,
            "pr": pr,
            "composite_score": round(composite, 2),
            "tier_badge": tier
        }

# ============================================================================
# PYTHON HARNESS
# ============================================================================
if __name__ == "__main__":
    print("=========================================================")
    print("  RentSphere Core Engine - Python 3.12 Polyglot Reference")
    print("=========================================================")

    drone = AutonomousMarineDrone(
        "AST-001", "Goa Coastal Deep-Survey Drone", "host_faaris",
        1200.0, 3000.0, "F2", 1.25, "IP68", 1.15, 150.0, True
    )

    # 1. RTTI Introspection
    reflection_info = inspect_runtime_entity(drone)
    print("\n[RTTI INTROSPECTION] Class Hierarchy:")
    print(" -> ".join(reflection_info["mro_inheritance_chain"]))

    # 2. Cloning Benchmark
    benchmark_cloning_memory(drone)

    # 3. NIRF Composite Calculation
    print("\n--- NIRF Composite Score Calculation ---")
    nirf_report = NirfCompositeScoreCalculator.calculate_score(
        tlr=78.5, rp=64.2, go=82.0, oi=70.5, pr=65.0
    )
    print(f"Goa University MCA Simulation: Composite Score = {nirf_report['composite_score']} | Tier: {nirf_report['tier_badge']}")
