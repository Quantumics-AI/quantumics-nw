import { Component, OnInit } from '@angular/core';
import { DataQuailtyListResponse } from '../../models/data-quality-list-response';
import { ActivatedRoute, Router } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable, Subject } from 'rxjs';
import { Certificate } from 'src/app/models/certificate';
import { Quantumfacade } from 'src/app/state/quantum.facade';
import { takeUntil } from 'rxjs/operators';
import { EditRuleComponent } from '../edit-rule/edit-rule.component';
import { SnackbarService } from 'src/app/core/services/snackbar.service';
import { ViewRunningRulesComponent } from '../view-running-rules/view-running-rules.component';
import { RuleCreationService } from '../../services/rule-creation.service';
import { RuleFilter } from '../../models/rule-filter.model';

@Component({
  selector: 'app-data-quality-list',
  templateUrl: './data-quality-list.component.html',
  styleUrls: ['./data-quality-list.component.scss']
})
export class DataQualityListComponent implements OnInit {

  private certificate$: Observable<Certificate>;
  private unsubscribe: Subject<void> = new Subject();
  private certificateData: Certificate;
  public loading:boolean;
  public ruleFilter = [
    { label: 'Inactive', name: 'Inactive', selected: false },
    { label: 'Deleted', name: 'Deleted', selected: false }
  ] as RuleFilter[];

  public dataQualityList: any;
  public ruleCount: any;
  public ruleCountInactive: any;
  public ruleCountDeleted: any;
  public projectId: number;
  userId: number;
  selectAllChecked: boolean = false;
  anyCheckboxSelected: boolean = false;

  // pagination
  public startIndex: number = 0;
  public pageSize: number = 100;
  public endIndex: number = this.pageSize;
  public page = 1;
  public rulesData: any;
  public paginationData: any;

  public ruleStatus: string = 'Active';
  public pageNumebr: number = 1;
  public pageLength: number = 100;

  public ruleId: any = [];
  public covertTime: any;
  public isDescending: boolean;
  public dateOption: any;
  public searchDiv: boolean = false;
  public searchString: string;
  searchTerm: any = { ruleName: '' };
  public timezone: any;

  constructor(
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private modalService: NgbModal,
    private quantumFacade: Quantumfacade,
    private snakbar: SnackbarService,
    private ruleCreationService: RuleCreationService) {
    this.projectId = +this.activatedRoute.snapshot.paramMap.get('projectId');
    this.certificate$ = this.quantumFacade.certificate$;
    this.certificate$
      .pipe(takeUntil(this.unsubscribe))
      .subscribe(certificate => {
        if (certificate) {
          this.certificateData = certificate;
          this.userId = +this.certificateData.user_id;
        }
      });
  }

  ngOnInit(): void {
    this.getRules();
    this.getRulesInactive();
    this.getRulesDelete();
    sessionStorage.clear();
  }

  public getRules(): void {
    this.loading = true;
    this.ruleCreationService.getRulesData(this.userId, this.projectId, this.ruleStatus, this.pageNumebr, this.pageLength).subscribe((response) => {
    
      this.loading = false;
      this.dataQualityList = response?.result?.content;
      this.ruleCount =  response?.result?.content;
      this.paginationData = response?.result;
      // this.pageSize = this.paginationData.size;
      // this.endIndex = this.pageSize;
      if (this.dataQualityList.length > 1) {
        this.dataQualityList.sort((val1, val2) => {
          return (
            (new Date(val2.createdDate) as any) -
            (new Date(val1.createdDate) as any)
          );
        });
      }
      
    }, (error) => {
      this.loading = false;
    });
  }

  public getRulesInactive(): void {
    this.loading = true;
    this.ruleCreationService.getRulesData(this.userId, this.projectId, 'Inactive', this.pageNumebr, this.pageLength).subscribe((response) => {
    
      this.ruleCountInactive =  response?.result?.content;
      
    }, (error) => {
      this.loading = false;
    });
  }

  public getRulesDelete(): void {
    this.loading = true;
    this.ruleCreationService.getRulesData(this.userId, this.projectId,'Deleted', this.pageNumebr, this.pageLength).subscribe((response) => {
      
      this.loading = false;
      this.ruleCountDeleted =  response?.result?.content;
      
    }, (error) => {
      this.loading = false;
    });
  }

  public edit(dataQuality: any): void {

    this.router.navigate([`projects/${this.projectId}/data-quality/edit`],{
      queryParams: {
        dataId: dataQuality.ruleId
      }
    });
  }

  public viewRunning(): void {
    this.router.navigate([`projects/${this.projectId}/data-quality/view-rules`]);
    // const modalRef = this.modalService.open(ViewRunningRulesComponent, { size: 'lg', windowClass: 'modal-size', scrollable: false });
    // modalRef.componentInstance.userId = this.userId;
    // modalRef.componentInstance.projectId = this.projectId;


    // modalRef.result.then((result) => {
      
    // }, (result) => {
      
    // });
  }

  selectRuleAll(event: Event): void {
    const isChecked = (event.target as HTMLInputElement).checked;
    this.selectAllChecked = isChecked;
    this.anyCheckboxSelected = isChecked;
    // Loop through your dataQualityList and update the selected state of each item
    // this.dataQualityList.map(dataQuality => {
    //   dataQuality.selected = isChecked;
    //   if (dataQuality.selected) {
    //     this.ruleId.push(dataQuality.ruleId);
    //   } else {
        
    //   }
    // });

    if (isChecked) {
      // If "Select All" is checked, add all ruleIds to the ruleIds array
      this.ruleId = this.dataQualityList.map(dataQuality => dataQuality.ruleId);
    } else {
      // If "Select All" is unchecked, clear the ruleIds array
      this.ruleId = [];
    }
  
    // Update the selected state of each item in the dataQualityList
    this.dataQualityList.forEach(dataQuality => {
      dataQuality.selected = isChecked;
    });
  }

  // Sort data 

  public sortRuleType(): void {
    this.isDescending = !this.isDescending;
    if (this.isDescending) {
      this.dataQualityList = this.dataQualityList.sort((a, b) => {
        var rule_name_order = a.ruleDetails?.ruleTypeName.localeCompare(b.ruleDetails?.ruleTypeName);
        return rule_name_order;
      });
    } else {
      this.dataQualityList = this.dataQualityList.sort((a, b) => {
        var rule_name_order = b.ruleDetails?.ruleTypeName.localeCompare(a.ruleDetails?.ruleTypeName);
        return rule_name_order;
      });
    }
  }
  
  public sortCreatedDate(): void {
    this.isDescending = !this.isDescending;
    if (this.isDescending) {
      this.dataQualityList.sort((a, b) => a.createdDate - b.createdDate);
    } else {
      this.dataQualityList.sort((a, b) => b.createdDate - a.createdDate);
    }
    
  }

  public sortModifiedDate(): void {
    this.isDescending = !this.isDescending;
    if (this.isDescending) {
      this.dataQualityList.sort((a, b) => a.modifiedDate - b.modifiedDate);
    } else {
      this.dataQualityList.sort((a, b) => b.modifiedDate - a.modifiedDate);
    }
    
  }

  selectRule(evt: Event, dataQuality: DataQuailtyListResponse): void {
    dataQuality.selected = (evt.target as HTMLInputElement).checked;
    if (dataQuality.selected) {
      // Add the ruleId to the ruleIds array if it's selected
      this.ruleId.push(dataQuality.ruleId);
    } else {
      // Remove the ruleId from the ruleIds array if it's deselected
    const index = this.ruleId.indexOf(dataQuality.ruleId);
    if (index !== -1) {
      this.ruleId.splice(index, 1);
    }
    }
    // Check if any checkbox is selected
    this.anyCheckboxSelected = this.dataQualityList.some(item => item.selected);
    // Check if all checkboxes are selected and update the "Select All" checkbox accordingly
    this.selectAllChecked = this.dataQualityList.every(item => item.selected);
    
  }

  public runRule(): void {
    this.dataQualityList.map(dataQuality => {
      dataQuality.selected = false;
    });
    this.anyCheckboxSelected = false;
    this.selectAllChecked = false;
    this.loading = true;
    const req = {
      ruleIds : this.ruleId
    }
    this.ruleCreationService.runRule(this.userId, this.projectId, req).subscribe((response) => {
      this.loading = false;
      this.ruleId = [];
      this.snakbar.open(response.message);
      
    }, (error) => {
      this.loading = false;
    });
  }

  public changeFilter(event: any, selectedFilter: RuleFilter): void {

    this.ruleFilter.forEach(filter => {
      if (filter !== selectedFilter) {
        filter.selected = false;
      }
    });
    selectedFilter.selected = event.target.checked;

    // Set the selected value as the name property or to null if both checkboxes are not selected
    if (this.ruleFilter.every(filter => !filter.selected)) {
      this.ruleStatus = 'Active';
    } else {
      this.ruleStatus = selectedFilter.name;
    }
    this.getStatusRules();
  }

  public getStatusRules(): void {
    this.loading = true;
    this.ruleCreationService.getRulesData(this.userId, this.projectId, this.ruleStatus, this.pageNumebr, this.pageLength).subscribe((response) => {
      
      this.dataQualityList = response?.result?.content;
      this.paginationData = response?.result;
      this.loading = false;
    }, (error) => {
      this.loading = false;
    });
  }
  

  // pagination 
  public onPageChange(currentPage: number): void {
    if(this.pageNumebr < currentPage){
      this.pageNumebr = currentPage;
    } else {
      this.pageNumebr = this.pageNumebr - currentPage;
    }
    this.getRules();
    // this.pageNumebr = 
    this.startIndex = (currentPage - 1) * this.pageSize;
    this.endIndex = this.startIndex + this.pageSize;
  }

  searchInput(str) {
    this.searchString = str;
    if (str.length == 0) {
      this.searchDiv = false;
    } else {
      this.searchDiv = true;
    }
  }

  convertToUKTime(createdDate: number): string {
    // Convert createdDate from IST to UK time
    const date = new Date(createdDate);
    
    const ukTimeZone = 'Europe/London'; // Timezone for the United Kingdom
    
    // Format the date in the desired format
    this.timezone = {
      timeZone: ukTimeZone,
      year: '2-digit',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      hour12: true,
    };

    // Use a custom format for the day without leading zero
    const ukDate = new Intl.DateTimeFormat('en-GB', this.timezone).format(date);
    const parts = ukDate.split('/');
    const formattedDate = parts.map((part, index) => (index === 0 ? part.slice(1) : part)).join('-');

    return formattedDate;
  }

  convertToUKTimeZone(timestamp: number) {
    if(timestamp) {
      const ukTimestamp = new Date(timestamp);

      // Check if Daylight Saving Time (DST) is in effect (typically from the last Sunday in March to the last Sunday in October)
      const today = new Date();
      const dstStart = new Date(today.getFullYear(), 2, 31 - ((5 * today.getFullYear() / 4 + 4) % 7), 1); // Last Sunday in March
      const dstEnd = new Date(today.getFullYear(), 9, 31 - ((5 * today.getFullYear() / 4 + 1) % 7), 1); // Last Sunday in October
      const isDST = today > dstStart && today < dstEnd;

      // Adjust for DST (1 hour ahead if DST is in effect)
      if (isDST) {
        ukTimestamp.setHours(ukTimestamp.getHours() + 1);
      }

      const day = ukTimestamp.getUTCDate();
      const month = ukTimestamp.getUTCMonth() + 1; // Months are zero-based, so add 1
      const year = ukTimestamp.getUTCFullYear() % 100; // Get the last two digits of the year
      const hours = ukTimestamp.getUTCHours();
      const minutes = ukTimestamp.getUTCMinutes();

      // Format date components with leading zeros if needed
      const formattedDate = `${day < 10 ? '0' : ''}${day}-${month < 10 ? '0' : ''}${month}-${year}`;
      const formattedTime = `${hours < 10 ? '0' : ''}${hours}:${minutes < 10 ? '0' : ''}${minutes}`;
      const amPm = hours < 12 ? 'AM' : 'PM';

      return `${formattedDate}, ${formattedTime} ${amPm}`;
    }
    
  }

  clearSearhInput() {
    this.searchTerm = { ruleName: '' };
    this.searchDiv = false;
  }
}
