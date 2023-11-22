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
import { RuleFilter, RuleListFilter } from '../../models/rule-filter.model';
import { BusinessDateComponent } from '../business-date/business-date.component';

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
  public filterBy = [
    { label: 'Date', name: 'date', selected: false },
    { label: 'Rule type', name: 'type', selected: false }
  ] as RuleListFilter[];

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
  public pageLength: number = 10;

  public ruleId: any = [];
  public covertTime: any;
  public isDescending: boolean;
  public dateOption: any;
  public searchDiv: boolean = false;
  public searchString: string;
  searchTerm: any = { ruleName: '' };
  public timezone: any;
  buttonDisabled: boolean = true;
  searchSuccessClass: string = 'search-success-btn';
  searchInvalidClass: string = 'search-disable-btn';
  public searchNull: boolean = false;
  public totalNumberOfRules: number;
  public selectedStatus: string = 'Active';
  public isActive: boolean;
  public selectedBusinessDate: any;

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
    // this.loading = true;
    this.selectedStatus = 'Active';
    this.ruleCreationService.getRulesData(this.userId, this.projectId, this.ruleStatus, this.pageNumebr, this.pageLength).subscribe((response) => {
    
      this.loading = false;
      this.dataQualityList = response?.result?.content;
      const hasInactiveOrDeleted = response?.result?.content.some(item => item.status === 'Inactive' || item.status === 'Deleted');
      this.isActive = !hasInactiveOrDeleted;
      this.ruleCount =  response?.result?.content;
      this.paginationData = response?.result;
      this.totalNumberOfRules = response?.result?.totalElements;
      
    }, (error) => {
      this.loading = false;
    });
  }

  public searchRule(): void {
    this.selectedStatus = 'Search'
    this.buttonDisabled = true;
    this.pageNumebr = 1;
    this.ruleCreationService.getSearchRule(this.userId, this.projectId, this.searchString, this.pageNumebr, this.pageLength).subscribe((response) => {
      
      if (response.code === 400) {
        this.searchNull = true;
        this.totalNumberOfRules = 0;
        this.paginationData = [];
      } 
      if(response.code === 200){
        this.searchNull = false;
        this.dataQualityList = response?.result?.content;
        const hasInactiveOrDeleted = response?.result?.content.some(item => item.status === 'Inactive' || item.status === 'Deleted');
        this.isActive = !hasInactiveOrDeleted;
        this.ruleCount =  response?.result?.content;
        this.paginationData = response?.result;
        this.totalNumberOfRules = response?.result?.totalElements;
      }
      
    }, (error) => {
      
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
    // this.loading = true;
    const req = {
      ruleIds : this.ruleId
    }
    
    const modalRef = this.modalService.open(BusinessDateComponent, { size: 'md modal-dialog-centered', scrollable: false});
    modalRef.componentInstance.userId = this.userId;
    modalRef.componentInstance.projectId = this.projectId;
    modalRef.componentInstance.ruleIds = req;


    modalRef.result.then((result) => {
      this.ruleId = [];
    }, (error) => {
      
    });
    // this.dataQualityList.map(dataQuality => {
    //   dataQuality.selected = false;
    // });
    // this.anyCheckboxSelected = false;
    // this.selectAllChecked = false;
    // this.loading = true;
    // const req = {
    //   ruleIds : this.ruleId
    // }

    // this.ruleCreationService.runRule(this.userId, this.projectId, req).subscribe((response) => {
    //   this.loading = false;
    //   this.ruleId = [];
    //   this.snakbar.open(response.message, '', 7000);
      
    // }, (error) => {
    //   this.loading = false;
    // });
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
      this.selectedStatus = 'Active';
    } else {
      this.ruleStatus = selectedFilter.name;
      this.selectedStatus = selectedFilter.name;
    }
    this.getStatusRules();
  }

  public getStatusRules(): void {
    this.loading = true;
    this.ruleCreationService.getRulesData(this.userId, this.projectId, this.ruleStatus, this.pageNumebr, this.pageLength).subscribe((response) => {
      
      this.dataQualityList = response?.result?.content;
      const hasInactiveOrDeleted = response?.result?.content.some(item => item.status === 'Inactive' || item.status === 'Deleted');
      this.isActive = !hasInactiveOrDeleted;
      this.paginationData = response?.result;
      this.totalNumberOfRules = response?.result?.totalElements;
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
      this.pageNumebr = currentPage;
    }
    this.getRules();
    // this.pageNumebr = 
    // this.startIndex = (currentPage - 1) * this.pageSize;
    // this.endIndex = this.startIndex + this.pageSize;
  }

  searchInput(str) {
    this.searchString = str;
    this.buttonDisabled = str.trim() === '';
    this.searchNull = false;
    this.getRules();
    
    if (str.length == 0) {
      this.searchDiv = false;
      // this.isSearch = false;
      this.searchNull = false;
      this.selectedStatus = 'Active';
      this.getRules();
    } else {
      this.searchDiv = true;
    }
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
    this.getRules();
    this.searchNull = false;
    this.searchTerm = { ruleName: '' };
    this.searchDiv = false;
    this.buttonDisabled = true;
  }

  public onChangeFilterBy(value: string): void {
    
  }
}
